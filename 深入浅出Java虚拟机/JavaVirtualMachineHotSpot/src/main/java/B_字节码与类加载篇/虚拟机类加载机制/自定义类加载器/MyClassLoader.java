package B_字节码与类加载篇.虚拟机类加载机制.自定义类加载器;

import java.io.*;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/17
 * @Description: 自定义类加载器
 */
public class MyClassLoader extends ClassLoader {

    /**
     * 字节码文件路径
     */
    private final String codeClassPath;

    public MyClassLoader(String codeClassPath) {
        this.codeClassPath = codeClassPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //字节码文件完全路径
        String path = codeClassPath + name + ".class";
        System.out.println(path);

        Class<?> aClass = null;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            int len = -1;
            byte[] bytes = new byte[1024];
            while ((len = bis.read(bytes)) != -1) {
                baos.write(bytes, 0, len);
            }
            byte[] classCode = baos.toByteArray();
            //用字节码流 创建 Class对象
            aClass = defineClass(null, classCode, 0, classCode.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aClass;
    }
}
