package com.jasper.DynamicInject;

import javax.tools.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

/**
 * 动态重新加载Class <br>
 * Java内置的ClassLoader总会在加载一个Class之前检查这个Class是否已经被加载过 <br>
 * 已经被加载过的Class不会加载第二次 <br>
 * 因此要想重新加载Class，我们需要实现自己的ClassLoader <br>
 * 另外一个问题是，每个被加载的Class都需要被链接(link)， <br>
 * 这是通过执行ClassLoader.resolve()来实现的，这个方法是 final的，无法重写。 <br>
 * ClassLoader.resolve()方法不允许一个ClassLoader实例link一个Class两次， <br>
 * 因此，当需要重新加载一个 Class的时候，需要重新New一个自己的ClassLoader实例。 <br>
 * 一个Class不能被一个ClassLoader实例加载两次，但是可以被不同的ClassLoader实例加载， <br>
 * 这会带来新的问题 <br>
 * 在一个Java应用中，Class是根据它的全名（包名+类名）和加载它的 ClassLoader来唯一标识的， <br>
 * 不同的ClassLoader载入的相同的类是不能互相转换的。 <br>
 * 解决的办法是使用接口或者父类，只重新加载实现类或者子类即可。 <br>
 * 在自己实现的ClassLoader中，当需要加载接口或者父类的时候，要代理给父ClassLoader去加载 <br>
 */

public class DynamicEngine {
    private static DynamicEngine ourInstance = new DynamicEngine();

    public static DynamicEngine getInstance() {
        return ourInstance;
    }
    private URLClassLoader parentClassLoader;
    private String classpath;
    private DynamicEngine() {
        this.parentClassLoader = (URLClassLoader) this.getClass().getClassLoader();
        this.buildClassPath();
    }
    private void buildClassPath() {
        this.classpath = null;
        StringBuilder sb = new StringBuilder();
        for (URL url : this.parentClassLoader.getURLs()) {
            String p = url.getFile();
            sb.append(p).append(File.pathSeparator);
        }
        this.classpath = sb.toString();
    }

    /**
     * java code转化为对象，成功则返回对象，失败则返回实例
     * 注意当有内部类的时候，优先编译内部类
     * @param fullClassName className
     * @param javaCode javaCode字符串
     * @return 实例
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Object javaCodeToObject(String fullClassName, String javaCode) throws IllegalAccessException, InstantiationException {
        long start = System.currentTimeMillis();
        Object instance = null;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
//        ClassFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(diagnostics, null, null));
        ClassFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(diagnostics, null, null));
        List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
        jfiles.add(new CharSequenceJavaFileObject(fullClassName, javaCode));

        List<String> options = new ArrayList<String>();
        options.add("-encoding");
        options.add("UTF-8");
        options.add("-classpath");
        options.add(this.classpath);

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, jfiles);
        boolean success = task.call();
        if (success) {
            JavaClassObject jco = fileManager.getMainJavaClassObject();
            DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(this.parentClassLoader);
            List<JavaClassObject> innerClassJcos = fileManager.getInnerClassJavaClassObject();
            if(innerClassJcos != null){
                for (JavaClassObject inner : innerClassJcos) {
                    String name = inner.getName();
                    name = name.substring(1, name.length() - 6);
                    dynamicClassLoader.loadClass(name, inner);
                }
            }
            Class clazz = dynamicClassLoader.loadClass(fullClassName,jco);
            instance = clazz.newInstance();
        } else {
            String error = "";
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                error = error + compilePrint(diagnostic);
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("javaCodeToObject use:"+(end-start)+"ms");
        return instance;
    }

    /**
     * 编译失败的时候，打印出失败的详细信息
     * @param diagnostic
     * @return
     */
    private String compilePrint(Diagnostic diagnostic) {
        System.out.println("Code:" + diagnostic.getCode());
        System.out.println("Kind:" + diagnostic.getKind());
        System.out.println("Position:" + diagnostic.getPosition());
        System.out.println("Start Position:" + diagnostic.getStartPosition());
        System.out.println("End Position:" + diagnostic.getEndPosition());
        System.out.println("Source:" + diagnostic.getSource());
        System.out.println("Message:" + diagnostic.getMessage(null));
        System.out.println("LineNumber:" + diagnostic.getLineNumber());
        System.out.println("ColumnNumber:" + diagnostic.getColumnNumber());
        StringBuffer res = new StringBuffer();
        res.append("Code:[" + diagnostic.getCode() + "]\n");
        res.append("Kind:[" + diagnostic.getKind() + "]\n");
        res.append("Position:[" + diagnostic.getPosition() + "]\n");
        res.append("Start Position:[" + diagnostic.getStartPosition() + "]\n");
        res.append("End Position:[" + diagnostic.getEndPosition() + "]\n");
        res.append("Source:[" + diagnostic.getSource() + "]\n");
        res.append("Message:[" + diagnostic.getMessage(null) + "]\n");
        res.append("LineNumber:[" + diagnostic.getLineNumber() + "]\n");
        res.append("ColumnNumber:[" + diagnostic.getColumnNumber() + "]\n");
        return res.toString();
    }
}

