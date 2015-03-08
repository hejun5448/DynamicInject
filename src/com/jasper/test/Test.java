package com.jasper.test;

import com.jasper.DynamicInject.DynamicEngine;

public class Test {

    public static void main(String[] arg){

        String className = "JavaCode";
        String src ="public class JavaCode{\n" +
                "    public void test(){\n" +
                "        System.out.println(123);\n" +
                "    }\n" +
                "}";
        System.out.println(src);
        DynamicEngine de = DynamicEngine.getInstance();
        try {
            Object instance =  de.javaCodeToObject(className,src);
            System.out.println(instance.getClass());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

}
