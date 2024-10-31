## 深入浅出JVM（一）之Hotspot虚拟机中的对象

本篇文章思维导图如下：

![image-20210330233053370.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8b20ab341597472cbce5796e3befcf51~tplv-k3u1fbpfcp-watermark.image?)


### 对象的创建

对象的创建可以分为五个步骤:**检查类加载,分配内存,初始化零值,设置对象头,执行实例构造器<init>**

#### 类加载检查

-   **HotSpot虚拟机遇到一条new指令,会先检查能否在常量池中定位到这个类的符号引用,检查这个类是否类加载过**

    -   没有类加载过就去类加载
    -   类加载过就进行下一步分配内存

#### 分配内存

**对象所需的内存在类加载完成后就可以完全确定**
    
    
    

##### 分配内存方式

虚拟机在堆上为新对象分配内存,有两种内存分配的方式:**指针碰撞,空闲列表**

-   指针碰撞

    -   使用场景: 堆内存规整整齐

    -   过程: 使用过的空间放在一边,空闲的空间放在另一边,中间有一个指针作为分界点指示器,把新生对象放在使用过空间的那一边,中间指针向空闲空间那边挪动一个新生对象的内存大小的距离即可

        

![image-20201028174425589.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0b70cf6d15b343ac840d18d5c3d2c196~tplv-k3u1fbpfcp-watermark.image?)

特点:*简单,高效*,因为要堆内存规整整齐,所以垃圾收集器应该要有*压缩整理*的能力

-   空闲列表

    -   使用场景: 已使用空间和空闲空间交错在一起
    -   过程: 虚拟机维护一个列表,列表中记录了哪些内存空间可用,分配时找一块足够大的内存空间划分给新生对象,然后更新列表
    -   特点: 比指针碰撞复杂, 但是对垃圾收集器可以不用压缩整理的能力

##### 分配内存流程

> 分配内存流程(栈--老年代--TLAB--Eden)

因为在堆上为对象分配内存,内存不足会引起GC,引起GC可能会有STW(Stop The World)影响响应

为了优化减少GC,当**对象不会发生逃逸(作用域只在方法中,不会被外界调用)且栈内存足够时,直接在栈上为对象分配内存**,当线程结束后,栈空间被回收,(局部变量也被回收)就不用进行垃圾回收了

开启逃逸分析`-XX:+DoEscapeAnalysis`满足条件的对象就在栈上分配内存

(当对象满足不会逃逸条件除了能够优化在栈上分配内存还会带来锁消除,标量替换等优化...)



![image-20201124164550072.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/565571652b5442999b7e1f0aa50956c2~tplv-k3u1fbpfcp-watermark.image?)
1.  尝试该对象能不能在栈上分配内存
2.  如果不符合1,且该对象特别的大,比如内存超过了JVM设置的大对象的值就直接在老年代上为它分配内存
3.  如果这个对象不大,为了解决并发分配内存,采用*TLAB 本地线程分配缓冲*

> TLAB 本地线程分配缓存

堆内存是线程共享的,并发情况下从堆中划分线程内存不安全,如果直接加锁会影响并发性能

为每个线程在Eden区分配小小一块属于线程的内存,类似缓冲区

**哪个线程要分配内存就在那个线程的缓冲区上分配,只有缓冲区满了,不够了才使用乐观的同步策略(CAS+失败重试)保证分配内存的原子性**



![image-123.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a7bbdac718d543daadccc47165e8433d~tplv-k3u1fbpfcp-watermark.image?)

​    

在并发情况下分配内存是不安全的(正在给A对象分配内存,指针还未修改,使用原来的指针为对象B分配内存),虚拟机**采用TLAB(Thread Local Allocation Buffer本地线程分配缓冲)和CAS+失败重试**来保证线程安全

-   TLAB：为每一个线程预先在伊甸园区（Eden）分配一块内存，JVM给线程中的对象分配内存时先在TLAB分配，直到对象大于TLAB中剩余的内存或TLAB内存已用尽时才需要同步锁定(也就是CAS+失败重试)
-   CAS+失败重试：采用CAS配上失败重试的方式保证更新操作的原子性

#### 初始化零值

**分配内存完成后,虚拟机将分配的内存空间初始化为零值**(不包括对象头) (零值: int对应0等)

**保证了对象的成员字段(成员变量)在Java代码中不赋初始值就可以使用**

#### 设置对象头

把一些信息(这个对象属于哪个类? 对象哈希码,对象GC分代年龄)存放在对象头中 (后面详细说明对象头)

#### 执行init方法

**init方法 = 实例变量赋值 + 实例代码块 + 实例构造器**

按照我们自己的意愿进行初始化

### 对象的内存布局

#### 对象内存信息

对象在堆中的内存布局可以分为三个部分:*对象头,实例数据,对齐填充*

-   对象头包括两类信息(8Byte + 4Byte)

     1. *Mark Word*:用于存储该对象自身运行时数据(该对象的**哈希码信息**,**GC信息**:分代年龄,**锁信息**:状态标志等)

     2. *类型指针(对象指向它类型元数据的指针)*:HotSpot通过类型指针确定该对象是哪个类的实例 (**如果该对象是数组,对象头中还必须记录数组的长度**)

    类型指针默认是压缩指针,内存超过32G时为了寻址就不能采用压缩指针了

-   实例数据是对象真正存储的有效信息

    1.  **记录从父类中继承的字段和该类中定义的字段**
    2.  父类的字段会出现在子类字段之前,默认子类较小的字段可以插入父类字段间的空隙以此来节约空间(`+XX:CompactFields`)

-   对齐填充

    HotSpot要求对象起始地址必须是8字节整倍数

    所以*任何对象的大小都必须是8字节的整倍*,如果对象实例数据部分未到达8字节就会通过对齐填充进行补全

#### 分析对象占用字节

> Object obj = new Object(); 占多少字节?

导入JOL依赖    

```xml
 <!-- https://mvnrepository.com/artifact/org.openjdk.jol/jol-core -->
         <dependency>
             <groupId>org.openjdk.jol</groupId>
             <artifactId>jol-core</artifactId>
             <version>0.12</version>
         </dependency>
```



![image-20201124171137539.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ca76928bc8c94aa5a9174e09663b5b32~tplv-k3u1fbpfcp-watermark.image?)
mark word : 8 byte

类型指针: 4 byte

对齐填充 12->16 byte

> int[] ints = new int[5]; 占多少内存?


![image-20201124171539813.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/95bb2fdcd4a9463687e1c86f3d6ea993~tplv-k3u1fbpfcp-watermark.image?)
mark word:8 byte

类型指针: 4 byte

数组长度: 4 byte

数组内容初始化: 4*5=20byte

对齐填充: 36 -> 40 byte

> 父类私有字段到底能不能被子类继承?

![image-20201124173533826.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cc784911a5654c999114535626af0321~tplv-k3u1fbpfcp-watermark.image?)
    
子类对象的内存空间中保存有父类私有字段，只是无法使用
    
    
    
    

#### 栈-堆-方法区结构图

![image-20210429190053375.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3532fcf38f7b498a96305092b02453cc~tplv-k3u1fbpfcp-watermark.image?)
### 对象的访问定位

**Java程序通过栈上的reference类型数据来操作堆上的对象**

> 访问方式

**对象实例数据: 对象的有效信息字段等(就是上面说的数据)**

**对象类型数据: 该对象所属类的类信息(存于方法区中)**

-   句柄访问

   

![image-20201109182633606.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/65bbbec01212441a8907fd65635da3f0~tplv-k3u1fbpfcp-watermark.image?)
    -   在堆中开辟一块内存作为句柄池,栈中的**reference数据存储的是该对象句柄池的地址**,**句柄中包含了对象实例数据和对象类型数据**
    -   **优点: 稳定,对象被移动时(压缩或复制算法),只需要改动该句柄的对象实例数据指针**
    -   **缺点: 多一次间接访问的开销**

-   直接指针访问

    

![image-20201109182806454.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e7a5b6055af94808a00599dd9a075241~tplv-k3u1fbpfcp-watermark.image?)
    

栈中的reference数据存储堆中该对象的地址(reference指向该对象),但是**对象的内存布局需要保存对象类型数据**
    
**优点: 访问速度快**
    
**缺点: 不稳定,对象被移动时(压缩或复制算法),需要改动指针**

访问方式是虚拟机来规定的,**Hotspot主要使用直接指针访问**
    
    
    
### 总结

本篇文章主要从对象的创建流程（类加载、分配内存、初始化零值、设置对象头、执行实例方法）、对象的内存布局（对象头、实例数据、对齐填充）、访问对象的定位方式（直接指针访问、句柄访问）等层面详细介绍了对象，还在其中穿插了栈上分配、TLAB等内存分配优化以及分析对象占用具体空间



## 深入浅出JVM（二）之运行时数据区和内存溢出异常


*Java虚拟机在运行Java程序时,把所管理的内存分为多个区域, 这些区域就是运行时数据区*

**运行时数据区可以分为:程序计数器,Java虚拟机栈,本地方法栈,堆和方法区**

![image-20201024102633402.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4ac46f39e3634741890defdd86386ef4~tplv-k3u1fbpfcp-watermark.image?)

### 程序计数器

Program Counter Register 程序记数寄存器

-   什么是程序计数器?

-   程序计数器是一块**很小的内存**,它可以当作**当前线程执行字节码的行号指示器**

-   程序计数器的作用是什么?

    1.  **字节码解释器通过改变程序计数器中存储的下一条字节码指令地址以此来达到流程控制**
    2.  Java多线程的线程会切换,为了保存线程切换前的正确执行位置,每个线程都应该有程序计数器,因此**程序计数器是线程私有的**


    线程**执行Java方法时,程序计数器记录的是正在执行的虚拟机字节码指令地址**
    
    线程**执行本地方法时,程序计数器记录的是空**


​    

    ![image-20210426193810777.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e339de9a8a404deaa32bbf7e508027f4~tplv-k3u1fbpfcp-watermark.image?)
    
    pc寄存器保存下一条要执行的字节码指令地址
    
    执行引擎根据pc寄存器找到对应字节码指令来使用当前线程中的局部变量表(取某个值)或操作数栈(入栈,出栈..)又或是将字节码指令翻译成机器指令,然后由CPU进行运算

-   生命周期

    -   因为程序计数器是线程私有的,所以**生命周期是随着线程的创建而创建,随着线程的消亡而消亡**

-   内存溢出异常

    -   **程序计数器是唯一一个没有OOM(OutOfMemoryError)异常的数据区**

### Java虚拟机栈

#### 简介

Java Virtual Mechine Stack

- Java虚拟机栈描述 **线程执行Java方法时的内存模型**

- Java虚拟机栈的作用

  -   方法被执行时,JVM会创建一个**栈帧(Stack Frame):用来存储局部变量,动态链接,操作数栈,方法出口等信息**
  -   **方法被调用到结束对应着栈帧在JVM栈中的入栈到出栈操作**

- 生命周期

  因为是**线程私有的,所以随着线程的创建而创建,随着线程的消亡而消亡**

**"栈"通常情况指的就是JVM栈**,更多情况下 **"栈"指的是JVM栈中的局部变量表**

-   局部变量表内容

    1.  八大基本数据类型

    2.  对象引用

        -   可以是指向对象起始地址的指针
        -   也可以是指向对象的句柄

    3.  returnAddress类型(指向字节码指令的地址)


局部变量表中的存储空间以 **局部变量槽(Slot)** 来表示, double和long 64位的用2个槽来表示,其他数据类型都是1个

内存空间是在编译期间就已经确定的,运行时不能更改

这里的局部变量槽真正的大小由JVM来决定

#### 运行时栈帧结构

> 结构图



![image-20210427124003834.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/016d0d1b653049b8bf5e0f30f1f4d1a4~tplv-k3u1fbpfcp-watermark.image?)
栈帧是Java虚拟机栈中的数据结构

Java虚拟机栈又是属于线程私有的

调用方法和方法结束 可以看作是 栈帧入栈,出栈操作

**Java虚拟机以方法作为最基本的执行单位**

*每个栈帧中包括: 局部变量表,操作数栈,栈帧信息(返回地址,动态连接,附加信息)*



![image-20201111211153926.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bc815b312a7d4de29ce4b1566c1a5a35~tplv-k3u1fbpfcp-watermark.image?)

从Java程序来看:在调用堆栈的所有方法都同时处于执行状态(比如:main方法中调用其他方法)

从执行引擎来看:**当前线程只有处于栈顶的栈帧才是当前栈帧,此栈帧对应的方法为当前方法,执行引擎所运行的字节码指令只针对当前栈帧***也就是执行引擎执行的字节码指令只针对栈顶栈帧(方法)*

```
     public void add(int a){
         a=a+2;
     }
     public static void main(String[] args) {
         new Test().add(10);
     }
```

![image-20201111202220713.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0e123069d6a64df2ba40d976b9c0ff21~tplv-k3u1fbpfcp-watermark.image?)

##### 局部变量表

**局部变量表用于存放方法中的`实际参数`和`方法内部定义的变量`(存储)**

**以局部变量槽为单位**(编译期间就确定了)

每个局部变量槽都可以存放`byte,short,int,float,boolean,reference,returnAddress`

byte,short,char,boolean在存储前转为int (boolean:0为false 非0为true)

而`double,long`由 两个局部变量槽存放

每个局部变量槽的真正大小应该是由JVM来决定的

> reference 和 returnAddress 类型是什么

-   **reference : 直接或间接的查找到对象实例数据(堆中)和对象类型数据(方法区)** 也就是通常说的引用
-   returnAddress: 曾经用来实现异常处理跳转,现在不用了,使用异常表代替

Java虚拟机通过`定位索引`的方式来使用局部变量表

局部变量表的范围: `0~max_locals-1`



![image-20201111205228487.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6506cd2ac2274e9ebc1ecf315a13cea5~tplv-k3u1fbpfcp-watermark.image?)
比如: 我们上面代码中add()方法只有一个int参数,也没有局部变量,为什么最大变量槽数量为2呢?

实际上: **默认局部变量槽中索引0的是方法调用者的引用(通过"this"可以访问这个对象)**

其余参数则**按照申明顺序**在局部变量槽的索引中

**槽的复用**:如果PC指令申明局部变量(j)已经超过了某个局部变量(a)的作用域,那么j就会复用a的slot



![image-20210426213307165.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e48bffbd2ae94a92a3f762b10aee4623~tplv-k3u1fbpfcp-watermark.image?)

##### 操作数栈

`max_stack`操作数栈的最大深度也是编译时就确定下来了的



![image-20201111205258587.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/298fc7a3f0e141148a95f780b8735605~tplv-k3u1fbpfcp-watermark.image?)
**在方法执行的时候(字节码指令执行),会往操作数栈中写入和提取内容**(比如add方法中`a=a+2`,a入栈,常数2入栈,执行相加的字节码指令,它们都出栈,然后把和再入栈)

**操作数栈中的数据类型必须与字节码指令匹配**(比如 `a=a+2`都是Int类型的,字节码指令应该是`iadd`操作int类型相加,而不能出现不匹配的情况)

*这是在类加载时验证阶段的字节码验证过程需要保证的*

##### 动态连接

**动态连接:栈帧中指向运行时常量池所属方法的引用**

> 静态解析与动态连接

符号引用转换为直接引用有两种方式

-   **静态解析:在类加载时解析阶段将符号引用解析为直接引用**


-   **动态连接:每次运行期间把符号引用解析为直接引用**(因为只有在运行时才知道到底指向哪个方法)

##### 方法返回地址

执行方法后,有两种方式可以退出

> 正常调用完成与异常调用完成

-   正常调用完成: 遇到方法返回的字节码指令

    -   方法退出有时需要在栈帧中保存一些信息以恢复上一层方法的执行状态(程序计数器的值)

-   异常调用完成: 遇到异常未捕获(未搜索到匹配的异常处理器)

    -   以异常调用完成方式退出方法,不会在栈帧中保存信息,通过异常处理器来确定

##### 附加信息

增加一些《Java虚拟机规范》中没有描述的信息在栈帧中(取决于具体虚拟机实现)

#### 模拟栈溢出

-   内存溢出异常

1.  线程请求栈深度大于JVM允许深度,抛出StackOverflowError异常
2.  栈扩展无法申请到足够内存,抛出OOM异常
3.  创建线程无法申请到足够内存,抛出OOM异常

关于栈的两种异常

1.  线程请求**栈深度大于JVM允许深度**,抛出StackOverflowError异常
2.  **栈扩展无法申请到足够内存** 或 **创建线程无法申请到足够的内存时**,抛出OOM异常

> 测试StackeOverflowError

另外在*hotSpot虚拟机中不区分虚拟机栈和本地方法栈*,所以`-Xoss`无效,只有`-Xss`设置单个线程栈的大小

```
 /**
  * @author Tc.l
  * @Date 2020/10/27
  * @Description: 测试栈溢出StackOverflowError
  * -Xss:128k 设置每个线程的栈内存为128k
  */
 public class StackSOF {
     private int depth=1;
 
     public void recursion(){
         depth++;
         recursion();
     }
 
     public static void main(String[] args) throws Throwable {
         StackSOF sof = new StackSOF();
         try {
             sof.recursion();
         } catch (Throwable e) {
             System.out.println("depth:"+sof.depth);
             throw e;
         }
     }
 }
 /*
 depth:1001
 Exception in thread "main" java.lang.StackOverflowError
     at 第2章Java内存区域与内存溢出.StackSOF.recursion(StackSOF.java:12)
     at 第2章Java内存区域与内存溢出.StackSOF.recursion(StackSOF.java:13)
     ...
     at 第2章Java内存区域与内存溢出.StackSOF.recursion(StackSOF.java:13)
     at 第2章Java内存区域与内存溢出.StackSOF.main(StackSOF.java:19)
 */
```

**减小了栈内存的空间,又递归调用频繁的创建栈帧,很快就会超过栈内存,从而导致StackOverflowError**

> 测试OOM

在我们经常使用的hotSpot虚拟机中是不支持栈扩展的

所以线程运行时不会因为扩展栈而导致OOM,只有可能是**创建线程无法申请到足够内存而导致OOM**

```
 /**
  * @author Tc.l
  * @Date 2020/10/27
  * @Description: 测试栈内存溢出OOM
  * -Xss2m 设置每个线程的栈内存为2m
  */
 public class StackOOM {
     public void testStackOOM(){
         //无限创建线程
         while (true){
             Thread thread = new Thread(new Runnable() {
                 @Override
                 public void run() {
                     //让线程活着
                     while (true) {
 
                     }
                 }
             });
             thread.start();
         }
     }
 
     public static void main(String[] args) {
         StackOOM stackOOM = new StackOOM();
         stackOOM.testStackOOM();
     }
 }
 
 /*
 Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
     at java.lang.Thread.start0(Native Method)
     at java.lang.Thread.start(Thread.java:717)
     at 第2章Java内存区域与内存溢出.StackOOM.testStackOOM(StackOOM.java:19)
     at 第2章Java内存区域与内存溢出.StackOOM.main(StackOOM.java:25)
 */
```

操作系统为(JVM)进程分配的内存大小是有效的,这个内存再减去堆内存,方法区内存,程序计数器内存,直接内存,虚拟机消耗内存等,剩下的就是虚拟机栈内存和本地方法栈内存

**此时增加了线程分配到的栈内存大小,又在无限建立线程,就很容易把剩下的内存耗尽,最终抛出OOM**

如果是因为这个原因出现的OOM,创建线程又是必要的,解决办法可以是*减小堆内存和减小线程占用栈内存大小*

### 本地方法栈

Native Method Stacks

与JVM栈作用类似

**JVM栈为Java方法服务**

**本地方法栈为本地方法服务**

**内存溢出异常也与JVM栈相同**

**hotspot将本地方法栈和Java虚拟机栈合并**

### Java 堆

#### 简介

-   什么是堆?

    -   堆是JVM内存管理中最大的一块区域

-   堆的作用是什么?

-   堆的目的就是**为了存放对象实例数据**

-   生命周期

    -   因为大部分对象实例都是存放在堆中,所以JVM启动时,堆就创建了 (注意这里的大部分,不是所有对象都存储在堆中)
    -   又因为线程都要去用对象,因此**堆是线程共享的**

-   堆内存

    -   **堆的内存在物理上是可以不连续的,在逻辑上是连续的**
    -   堆内存可以是固定的,也是扩展(-Xmx , -Xms)

#### 堆的内存结构



![image-20210427203238511.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/445ce5eaab734f8e8d99d99c68496dce~tplv-k3u1fbpfcp-watermark.image?)

- 年轻代

  -   伊甸园区(eden)

      -   大部分对象都是伊甸园区被new出来的

  -   幸存to区( Survive to)

  -   幸存from区( Survive from)

- 老年代

- 永久代(JDK8后变为元空间)

  - 常驻内存，用来存放JDK自身携带的Class对象，存储的是Java运行时的一些环境

  - JDK 6之前：永久代，静态常量池在方法区

  - JDK 7 ： 永久代，慢慢退化，`去永久代`，将静态常量池移到堆中（字符串常量池也是）

  - JDK 8后 ：**无永久代，方法，静态常量池在元空间，元空间仍与堆不相连，但与堆共享物理内存，逻辑上可认为在堆中**

  - 不存在垃圾回收，关闭JVM就会释放这个区域的内存

  - 什么情况下永久代会崩：

    -   一个启动类加载大量第三方jar包
    -   tomcat部署太多应用
    -   大量动态生成反射类，不断被加载直到内存满，就出现OOM

    **因为这些原因容易OOM所以将永久代换成元空间，使用本地内存**



![image-20200331222733967.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5f3892be70824feeb0292e8379b273df~tplv-k3u1fbpfcp-watermark.image?)
**元空间：逻辑上存在堆，物理上不存在堆**（使用本地内存）

GC垃圾回收主要在**伊甸园区，老年区**

#### 内存调优

##### 堆内存常用参数

| 指令                     | 作用                                                         |
| ------------------------ | ------------------------------------------------------------ |
| -Xms                     | 设置初始化内存大小 默认1/64                                  |
| -Xmx                     | 设置最大分配内存 默认1/4                                     |
| -XX:+PrintGCDetails      | 输出详细的GC处理日志                                         |
| -XX:NewRatio = 2         | 设置老年代占堆内存比例 **默认新生代:老年代=1:2**(新生代永远为1,设置的值是多少老年代就占多少) |
| -XX:SurvivorRatio = 8    | 设置eden与survivor内存比例 **文档上默认8:1:1实际上6:1:1**(设置的值是多少eden区就占多少) |
| -Xmn                     | 设置新生代内存大小                                           |
| -XX:MaxTenuringThreshold | 设置新生代去老年代的阈值                                     |
| -XX:+PrintFlagsInitial   | 查看所有参数默认值                                           |
| -XX:+PrintFlagsFinal     | 查看所有参数最终值                                           |

##### 查看堆内存

```
 public class HeapTotal {
     public static void main(String[] args) {
         //JVM试图使用最大内存
         long maxMemory = Runtime.getRuntime().maxMemory();
         //JVM初始化总内存
         long totalMemory = Runtime.getRuntime().totalMemory();
 
         System.out.println("JVM试图使用最大内存-->"+maxMemory+"KB 或"+(maxMemory/1024/1024)+"MB");
         System.out.println("JVM初始化总内存-->"+totalMemory+"KB 或"+(totalMemory/1024/1024)+"MB");
         /*
         JVM试图使用最大内存-->2820669440KB 或2690MB
         JVM初始化总内存-->191365120KB 或182MB
         */
     }
 }
```

**默认情况下 JVM试图使用最大内存是电脑内存的1/4 JVM初始化总内存是电脑内存的1/64 （电脑内存：12 G）**

##### 修改堆内存

> 使用-Xms1024m -Xmx1024m -XX:+PrintGCDetails 执行HeapTotal

```
 JVM试图使用最大内存-->1029177344B 或981MB
 JVM初始化总内存-->1029177344B 或981MB
 Heap
  PSYoungGen      total 305664K, used 15729K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
   eden space 262144K, 6% used [0x00000000eab00000,0x00000000eba5c420,0x00000000fab00000)
   from space 43520K, 0% used [0x00000000fd580000,0x00000000fd580000,0x0000000100000000)
   to   space 43520K, 0% used [0x00000000fab00000,0x00000000fab00000,0x00000000fd580000)
  ParOldGen       total 699392K, used 0K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
   object space 699392K, 0% used [0x00000000c0000000,0x00000000c0000000,0x00000000eab00000)
  Metaspace       used 3180K, capacity 4496K, committed 4864K, reserved 1056768K
   class space    used 343K, capacity 388K, committed 512K, reserved 1048576K
```

**最好-Xms初始化分配内存与-Xmx最大分配内存一致,因为扩容需要开销**

为什么明明设置的是1024m 它显示使用的是981m?

**因为幸存from,to区采用复制算法,总有一个幸存区的内存会被浪费**

**年轻代内存大小 = eden + 1个幸存区 (305664 = 262144 + 43520)**

**堆内存大小 = 年轻代内存大小 + 老年代内存大小 (305664 + 699392 = 1005056KB/1024 = 981MB)**

所以说: **元空间逻辑上存在堆内存，但是物理上不存在堆内存**

#### 模拟堆OOM异常

因为堆是存放对象实例的地方,所以只需要不断的创建对象

并且让`GC Roots`到各个对象间有可达路径来避免清除这些对象(因为用可达性分析算法来确定垃圾)

最终就可以导致堆内存没有内存再为新创建的对象分配内存,从而导致OOM

```
 /**
  * @author Tc.l
  * @Date 2020/10/27
  * @Description: 测试堆内存溢出
  */
 public class HeapOOM {
     /**
      * -Xms20m 初始化堆内存
      * -Xmx20m 最大堆内存
      * -XX:+HeapDumpOnOutOfMemoryError Dump出OOM的内存快照
      */
     public static void main(String[] args) {
         ArrayList<HeapOOM> list = new ArrayList<>();
         while (true){
             list.add(new HeapOOM());
         }
     }
 }
 
 /*
 java.lang.OutOfMemoryError: Java heap space
 Dumping heap to java_pid17060.hprof ...
 Heap dump file created [28270137 bytes in 0.121 secs]
 Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
     at java.util.Arrays.copyOf(Arrays.java:3210)
     at java.util.Arrays.copyOf(Arrays.java:3181)
     at java.util.ArrayList.grow(ArrayList.java:265)
     at java.util.ArrayList.ensureExplicitCapacity(ArrayList.java:239)
     at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:231)
     at java.util.ArrayList.add(ArrayList.java:462)
     at 第2章Java内存区域与内存溢出.HeapOOM.main(HeapOOM.java:20)
 */
```

解决这个内存区域的异常的常用思路:

-   确定内存中导致出现OOM的对象是否必要(*确定是内存泄漏还是内存溢出*)

    -   **内存泄漏: 使用内存快照工具找到泄漏对象到GC Roots的引用类,找出泄漏原因**

    -   **内存溢出: 根据物理内存试试能不能再把堆内存调大些,减少生命周期过长等设计不合理的对象,降低内存消耗**

### 方法区

#### 简介

> Method Area

- 什么是方法区?

  -   方法区在逻辑上是堆的一个部分,但在物理上不是,又名"非堆"(Non Heap)就是为了区分堆

- 方法区的作用是什么?

  -   方法区用来存储**类型信息,常量,静态变量,即时编译器编译后的代码缓存**等数据

  也和堆一样可以固定内存也可以扩展

- 生命周期

  -   因为存储了类型信息,常量,静态变量等信息,很多信息线程都会使用到,因此**方法区也是一个线程共享的区域**

- 历史

  -   JDK 6 前 HotSpot设计团队使用"永久代"来实现方法区

  -   Oracle收购BEA后,想把JRockit的优秀功能移植到HotSpot,但是发现JRockit与HotSpot内部实现不同,没有永久代(并且发现永久代更容易遇到内存溢出问题)

  -   JDK 6 计划放弃永久代,逐步改为采用*本地内存*(Native Memory)来实现方法区

  -   JDK 7 把永久代中的***字符串常量池,静态变量等* 移出到堆中**

      -   为什么要把字符串常量池放到堆中?

          -   **字符串常量池在永久代只有FULL GC才可以被回收,开发中会有大量字符串被创建,方法区回收频率低,放在堆中回收频率高**

  -   JDK 8 完全废弃永久代,改用与JRockit , J9 一样的方式**采用本地内存中实现的元空间来代替**,把原本永久代中剩下的信息(类型信息)全放在元空间中

- 内存溢出异常

  -   方法区无法满足新的内存分配时,抛出OOM异常

#### 模拟方法区OOM异常

因为方法区的主要责任是用于存放相关类信息,只需要运行时产生大量的类让方法区存放,直到方法区内存不够抛出OOM

使用CGlib操作字节码运行时生成大量动态类

导入CGlib依赖

```
         <dependency>
             <groupId>cglib</groupId>
             <artifactId>cglib-nodep</artifactId>
             <version>3.3.0</version>
         </dependency>
```

```
 /*
  * -XX:MaxMetaspaceSize=20m 设置元空间最大内存20m
  * -XX:MetaspaceSize=20m    设置元空间初始内存20m
  */
 public class JavaMethodOOM {
     public static void main(String[] args) {
         while (true){
             Enhancer enhancer = new Enhancer();
             enhancer.setSuperclass(JavaMethodOOM.class);
             enhancer.setUseCache(false);
             enhancer.setCallback(new MethodInterceptor() {
                 @Override
                 public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                     return proxy.invokeSuper(obj, args);
                 }
             });
             enhancer.create();
         }
     }
 }
 
 /*
 Caused by: java.lang.OutOfMemoryError: Metaspace
     at java.lang.ClassLoader.defineClass1(Native Method)
     at java.lang.ClassLoader.defineClass(ClassLoader.java:763)
     ... 11 more
 */
```

很多主流框架(Spring)对类增强时都会用到这类字节码技术

所以增强的类越多,存放在方法区就越容易溢出

#### 运行时常量池

> Runtime Constant Pool

- 什么是运行时常量池?

  -   运行时常量池是方法区中的一部分

- 运行时常量池的作用是什么?

  -   类加载后,将Class文件中常量池表(Constant Pool Table)中的**字面量和符号引用**保存到运行时常量池中

  

![image-20210428225851302.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/af7bde9911e7454bb8c1b2b2e0df21cd~tplv-k3u1fbpfcp-watermark.image?)
    符号引用:#xx 会指向常量池中的一个直接引用(比如类引用Object)
    
并且会把**符号引用翻译成直接引用保存在运行时常量池**中

**运行时也可以将常量放在运行时常量池(String的intern方法)**

运行时常量池中,**绝大部分是随着JVM运行,从常量池中转化过来的,还有部分可能是通过动态放进来的(String的intern)**

-   生命周期和内存溢出异常

    -   因为是方法区的一部分所以与方法区相同

### 直接内存

#### 简介

> Direct Memory

直接内存不是运行时数据区的一部分,因为这部分内存被频繁使用,有可能导致抛出OOM

Java1.4加入了NIO类,引入了以*通道传输,缓冲区存储*的IO方式

它可以让本地方法库直接分配物理内存,通过一个在Java堆中`DirectByteBuffer`的对象作为这块物理内存的引用进行IO操作 **避免在Java堆中和本地物理内存堆中来回copy数据**

直接内存分配不受Java堆大小的影响,如果忽略掉直接内存,使得各个内存区域大小总和大于物理内存限制,扩展时就会抛出OOM

#### 测试分配直接内存

```
public class LocalMemoryTest {
    private static final int BUFFER = 1024 * 1024 * 1024 ;//1GB

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
        System.out.println("申请了1GB内存");

        System.out.println("输入任意字符释放内存");

        Scanner scanner = new Scanner(System.in);
        scanner.next();
        System.out.println("释放内存成功");
        buffer=null;
        System.gc();
        while (!scanner.next().equalsIgnoreCase("exit")){

        }
        System.out.println("退出程序");
    }
}
```



#### 模拟直接内存溢出

默认直接内存与最大堆内存一致

`-XX:MaxDirectMemorySize`可以修改直接内存

使用NIO中的`DirectByteBuffer`分配直接内存也会抛出内存溢出异常,但是它抛出异常并没有真正向操作系统申请空间,只是通过计算内存不足,自己手动抛出的异常

**真正申请分配直接内存的方法是Unsafe::allocateMemory()**



![image-20210429192310660.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7b6020da480340afa058442dc33fbe31~tplv-k3u1fbpfcp-watermark.image?)

```
/* 测试直接内存OOM
 * -XX:MaxDirectMemorySize=10m
 * -Xmx20m
 */
public class DirectMemoryOOM {
    static final int _1MB = 1024*1024;
    public static void main(String[] args) throws IllegalAccessException {
        Field declaredField = Unsafe.class.getDeclaredFields()[0];
        declaredField.setAccessible(true);
        Unsafe unsafe  =(Unsafe) declaredField.get(null);
        while (true){
            unsafe.allocateMemory(_1MB);
        }
    }
}
```

由直接内存出现的OOM的明显特征就是:Dump 堆快照中,没有什么明显的异常

如果是这种情况,且使用了NIO的直接内存可以考虑这方面的原因

### 本地方法接口与本地方法库

**本地方法: 关键字`native`修饰的方法,Java调用非Java代码的接口**

**注意: `native`不能和`abstract`一起修饰方法**

> 为什么需要本地方法

1.  Java需要调用其他语言 (C,C++等)
2.  Java要与操作系统交互 (JVM部分也是由C实现)

**本地方法很少了,部分都是与硬件有关**(比如启动线程`start0()`)

只是部分虚拟机支持本地方法

> 本地方法接口

**本地方法通过本地方法接口来访问虚拟机中的运行时数据区**

某线程调用本地方法时,它就不受虚拟机的限制,在OS眼里它和JVM有同样权限

可以直接使用本地处理器中的寄存器,直接从本地内存分配任意内存

> 本地方法库

本地方法栈中登记`native`修饰的方法,由执行引擎来加载本地方法库

### 总结



![image-20210430190903437.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a3fb8d9b85154c0fa16cec5dbb92d2f3~tplv-k3u1fbpfcp-watermark.image?)

本片文章详细说明jvm运行时内存区域以及可能发生的内存溢出异常

**线程私有的程序计数器保存要执行的字节码指令，程序计数器不会发生内存溢出异常**

**线程私有的栈服务于方法，每个方法代表一个栈帧，方法的调用与调用结束标志着栈帧的入栈与出栈，栈帧中的局部变量表、操作数栈、方法返回地址、动态连接（运行时常量池引用）、附加信息是为了帮助更好的服务方法，栈；hospot虚拟机中栈可能存在栈溢出异常（递归调用无终止条件），也可能存在内存溢出异常（当创建线程无法分配内存时）**

**线程私有的本地栈与栈的区别就是，本地栈用于服务本地方法**

**线程公有的堆服务于存储对象，大部分对象都存储在堆中，堆内存划分为新生代（伊甸园区、幸存0区、1区）、老年代、元空间（直接内存），对象生命周期长短不一，新生代与老年代GC时使用的算法也不同；当堆内存不足，无法给新对象分配内存时，发生内存溢出异常（堆内存OOM需要排查的是到底是内存不足还是发生了内存泄漏）**

**线程公有的方法区存储类相关信息和运行时常量池，运行时常量池存放常量和引用，符号引用和动态连接会指向运行时常量池的直接引用；如果大量加载类信息，方法区也会发生内存溢出异常**

**直接内存也有可能发生内存溢出异常，当发生内存溢出异常时，堆内存没有异常可能是直接内存的原因**





## 深入浅出JVM（三）之HotSpot虚拟机类加载机制

### HotSpot虚拟机类加载机制

#### 类的生命周期

> 什么叫做类加载?

**类加载的定义: JVM把描述类的数据从Class文件加载到内存,并对数据进行校验,解析和初始化,最终变成可以被JVM直接使用的Java类型**(因为可以动态产生,这里的Class文件并不是具体存在磁盘中的文件,而是二进制数据流)

一个类型被加载到内存使用 到 结束卸载出内存,它的生命周期分为7个阶段: 加载->验证->准备->解析->初始化->使用->卸载

其中重要阶段一般的**开始顺序**: *加载->验证->准备->解析->初始化*

验证,准备,解析合起来又称为连接所以也可以是*加载->连接->初始化*

注意这里的顺序是一般的开始顺序,并不一定是执行完某个阶段结束后才开始执行下一个阶段,也可以是执行到某个阶段的中途就开始执行下一个阶段

还有种特殊情况就是**解析可能在初始化之后(因为Java运行时的动态绑定)**

**基本数据类型不需要加载,引用类型才需要被类加载**

#### 类加载阶段

接下来将对这五个阶段进行详细介绍

> Loading

##### 加载

-   加载的作用

1.  **通过这个类的全限定名来查找并加载这个类的二进制字节流**

    -   JVM通过文件系统加载某个class后缀文件
    -   读取jar包中的类文件
    -   数据库中类的二进制数据
    -   使用类似HTTP等协议通过网络加载
    -   运行时动态生成Class二进制数据流

2.  **将这个类所代表的静态存储结构(静态常量池)转化为方法区运行时数据结构(运行时常量池)**

3.  **在堆中创建这个类的Class对象,这个Class对象是对方法区访问数据的"入口"**

    -   堆中实例对象中对象头的类型指针指向它这个类方法区的类元数据

-   对于加载可以由JVM的自带类加载器来完成,也可以通过开发人员自定义的类加载器来完成(实现ClassLoader,重写findClass())

注意

1.  数组类是直接由JVM在内存中动态构造的,数组中的元素还是要靠类加载器进行加载
2.  反射正是通过加载创建的Class对象才能在运行期使用反射

> Verification

##### 验证

- 验证的作用

  **确保要加载的字节码符合规范,防止危害JVM安全**

- 验证的具体划分

  - 文件格式验证

    **目的: 保证字节流能正确解析并存储到方法区之内,格式上符合Java类型信息**

    验证字节流是否符合Class文件格式规范(比如Class文件主,次版本号是否在当前虚拟机兼容范围内...)

  - 元数据验证

    **目的: 对类的元数据信息进行语义验证**

    元数据：简单的来说就是描述这个类与其他类之间关系的信息

    元数据信息验证(举例):

    1.  这个类的父类有没有继承其他的最终类(被final修饰的类,不可让其他类继承)
    2.  若这个类不是抽象类,那这个类有没有实现(抽象父类)接口的所有方法

  - 字节码验证(验证中最复杂的一步)

    **目的: 对字节码进行验证,保证校验的类在运行时不会做出对JVM危险的行为**

    字节码验证举例:

    1.  类型转换有效: 子类转换为父类(安全,有效) 父类转换为子类(危险)
    2.  进行算术运算,使用的是否是相同类型指令等

  - 符号引用验证

    发生在解析阶段前:符号引用转换为直接引用

    **目的: 保证符号引用转为直接引用时,该类不缺少它所依赖的资源(外部类),确保解析可以完成**

验证阶段是一个非常重要的阶段,但又不一定要执行(因为许多第三方的类,自己封装的类等都被反复"实验"过了)

在生产阶段可以考虑关闭 *-Xverify:none*以此来缩短类加载时间

> Preparation

##### 准备

**准备阶段为类变量(静态变量)分配内存并默认初始化**

- 分配内存

  -   逻辑上应该分配在方法区,但是因为hotSpot在JDK7时将*字符串常量,静态变量*挪出永久代(放在堆中)
  -   实际上它应该在堆中

- 默认初始化

  - 类变量一般的默认初始化都是初始化该类型的*零值*

    | 类型      | 零值     |
    | --------- | -------- |
    | byte      | (byte)0  |
    | short     | (short)0 |
    | int       | 0        |
    | long      | 0L       |
    | float     | 0.0F     |
    | double    | 0.0      |
    | boolean   | false    |
    | char      | '\u0000' |
    | reference | null     |

  - 特殊的类变量的字段属性中存在*ConstantValue*属性值,会初始化为ConstantValue所指向在常量池中的值

  - ***只有被final修饰的基本类型或字面量且要赋的值在常量池中才会被加上`ConstantValue`属性***

      

![](https://bbs-img.huaweicloud.com/blogs/img/20240927/1727399257324109193.png)



> Resolution

##### 解析

- 解析的作用

  将常量池中的常量池中**符号引用替换为直接引用**（把符号引用代表的地址替换为真实地址）

  -   符号引用

      -   **使用一组符号描述引用**(为了定位到目标引用)
      -   与虚拟机内存布局无关
      -   还是符号引用时目标引用不一定被加载到内存

  -   直接引用

      -   **直接执行目标的指针,相对偏移量或间接定位目标引用的句柄**
      -   与虚拟机内存布局相关
      -   解析直接引用时目标引用已经被加载到内存中

- 并未规定解析的时间

  可以是类加载时就对常量池的符号引用解析为直接引用

  也可以在符号引用要使用的时候再去解析(动态调用时只能是这种情况)

- 同一个符号引用可能会被解析多次,所以会有缓存(标记该符号引用已经解析过),多次解析动作都要保证每次都是相同的结果(成功或异常)

###### 类和接口的解析

当我们要访问一个未解析过的类时

1.  把要解析的类的符号引用 交给当前所在类的类加载器 去加载 这个要解析的类
2.  解析前要进行符号引用验证,如果当前所在类没有权限访问这个要解析的类,抛出异常`IllegalAccessError`

###### 字段的解析

解析一个从未解析过的字段

1.  先对此字段所属的类(类, 抽象类, 接口)进行解析

2.  然后在此字段所属的类中查找该字段简单名称和描述符都匹配的字段,返回它的直接引用

    -   如果此字段所属的类有父类或实现了接口,要自下而上的寻找该字段
    -   找不到抛出`NoSuchFieldError`异常

3.  对此字段进行权限验证(如果不具备权限抛出`IllegalAccessError`异常)

**确保JVM获得字段唯一解析结果**

如果同名字段出现在父类,接口等中,编译器有时会更加严格,直接拒绝编译Class文件

###### 方法的解析

解析一个从未解析过的方法

1.  先对此方法所属的类(类, 抽象类, 接口)进行解析

2.  然后在此方法所属的类中查找该方法简单名称和描述符都匹配的方法,返回它的直接引用

    -   如果此方法所属类是接口直接抛出`IncompatibleClassChangeError`异常
    -   如果此方法所属的类有父类或实现了接口,要自下而上的寻找该方法(先找父类再找接口)
    -   如果在接口中找到了,说明所属类是抽象类,抛出`AbstractMethodError`异常(自身找不到,父类中找不到,最后在接口中找到了,说明他是抽象类),找不到抛出`NoSuchMethodError`异常

3.  对此方法进行权限验证(如果不具备权限抛出`IllegalAccessError`异常)

###### 接口方法的解析

解析一个从未解析过的接口方法

1.  先对此接口方法所属的接口进行解析

2.  然后在此接口方法所属的接口中查找该接口方法简单名称和描述符都匹配的接口方法,返回它的直接引用

    -   如果此接口方法所属接口是类直接抛出`IncompatibleClassChangeError`异常
    -   如果此方法所属的接口有父接口,要自下而上的寻找该接口方法
    -   如果多个不同的接口中都存在这个接口方法,会随机返回一个直接引用(编译会更严格,这种情况应该会拒绝编译)

3.  找不到抛出`NoSuchMethodError`

> Initializtion

##### 初始化

**执行类构造器<clinit>的过程**

- 什么是<clinit> ?

  -   <clinit>是**javac编译器 在编译期间自动收集类变量赋值的语句和静态代码块合并 自动生成的**
  -   如果没有对类变量赋值动作或者静态代码块<clinit>可能不会生成 (带有`ConstantValue`属性的类变量初始化已经在准备阶段做过了,不会在这里初始化)

- 类和接口的类构造器

  - 类

    <clinit>又叫类构造器,与<init>实例构造器不同,类构造器不用显示父类类构造器调用

    但是**父类要在子类之前初始化**,也就是完成类构造器

  - 接口

    执行接口的类构造器时,不会去执行它父类接口的类构造器,直到用到父接口中定义的变量被使用时才执行

- JVM会保证执行<clinit>在多线程环境下被正确的加锁和同步(也就是只会有一个线程去执行<clinit>其他线程会阻塞等待,直到<clinit>完成)

  ```java
   public class TestJVM {
       static class  A{
           static {
               if (true){
                   System.out.println(Thread.currentThread().getName() + "<clinit> init");
                   while (true){
   
                   }
               }
           }
       }
       @Test
       public void test(){
           Runnable runnable = new Runnable() {
               @Override
               public void run() {
                   System.out.println(Thread.currentThread().getName() + "start");
                   A a = new A();
                   System.out.println(Thread.currentThread().getName() + "end");
               }
           };
   
           new Thread(runnable,"1号线程").start();
           new Thread(runnable,"2号线程").start();
       }
   
   }
   
   /*
   1号线程start
   2号线程start
   1号线程<clinit> init
   */
  ```

> JVM规定6种情况下必须进行初始化(主动引用)

###### 主动引用

-   遇到*new,getstatic,putstatic,invokestatic*四条字节码指令

    -   new
    -   读/写 某类静态变量(不包括常量)
    -   调用 某类静态方法

-   使用`java.lan.reflect`包中方法对类型进行反射

-   父类未初始化要先初始化父类 (不适用于接口)

-   虚拟机启动时,先初始化main方法所在的类

-   某类实现的接口中有默认方法(JDK8新加入的),要先对接口进行初始化

-   JDK7新加入的动态语言支持,部分....

###### 被动引用

> 1.  当访问静态字段时,只有真正声明这个字段的类才会被初始化

(子类访问父类静态变量)

```java
 public class TestMain {
     static {
         System.out.println("main方法所在的类初始化");
     }
 
     public static void main(String[] args) {
         System.out.println(Sup.i);
     }
 }
 
 class Sub{
     static {
         System.out.println("子类初始化");
     }
 }
 
 class Sup{
     static {
         System.out.println("父类初始化");
     }
     static int i = 100;
 }
 
 /*
 main方法所在的类初始化
 父类初始化
 100
 */
```

**子类调用父类静态变量是在父类类加载初始化的时候赋值的,所以子类不会类加载**

> 2.  实例数组

```java
 public class TestArr {
     static {
         System.out.println("main方法所在的类初始化");
     }
     public static void main(String[] args) {
         Arr[] arrs = new Arr[1];
     }
 }
 
 class Arr{
     static {
         System.out.println("arr初始化");
     }
 }
 
 /*
 main方法所在的类初始化
 */
```

例子里包名为：org.fenixsoft.classloading。该例子没有触发类org.fenixsoft.classloading.Arr的初始化阶段，但触发了另外一个名为“[Lorg.fenixsoft.classloading.Arr”的类的初始化阶段，对于用户代码来说，这并不是一个合法的类名称，它是一个**由虚拟机自动生成的、直接继承于Object的子类，创建动作由字节码指令anewarray触发.** 这个类**代表了一个元素类型为org.fenixsoft.classloading.Arr的一维数组**，数组中应有的属性和方法（用户可直接使用的只有被修饰为public的length属性和clone()方法）都实现在这个类里。

**创建数组时不会对数组中的类型对象(Arr)发生类加载**

**虚拟机自动生成的一个类,管理Arr的数组,会对这个类进行类加载**

> 3.  调用静态常量

```java
 public class TestConstant {
     static {
         System.out.println("main方法所在的类初始化");
     }
     public static void main(String[] args) {
         System.out.println(Constant.NUM);
     }
 }
 
 class Constant{
     static {
         System.out.println("Constant初始化");
     }
     static final int NUM = 555;
 }
 
 /*
 main方法所在的类初始化
 555
 */
```

我们在连接阶段的准备中说明过,如果静态变量字段表中有`ConstantValue`(被final修饰)它在准备阶段就已经完成初始默认值了,不用进行初始化

> 4.  调用classLoader类的loadClass()方法加载类不导致类初始化



 ![](https://bbs-img.huaweicloud.com/blogs/img/20240927/1727399300828445827.png)


##### 卸载

方法区的垃圾回收主要有两部分: *不使用的常量和类*

回收方法区性价比比较低,因为不使用的常量和类比较少

> 不使用的常量

**没有任何地方引用常量池中的某常量**,则该常量会在垃圾回收时,被收集器回收

> 不使用的类

成为不使用的类需要满足以下要求:

0.  **没有该类的任何实例对象**
1.  **加载该类的类加载器被回收**
2.  **该类对应的Class对象没在任何地方被引用**

注意: 就算被允许回收也不一定会被回收, 一般只会回收自定义的类加载器加载的类





### 总结

本篇文章围绕类加载阶段流程的加载-验证-准备-解析-初始化-卸载 详细展开每个阶段的细节

**加载阶段主要是类加载器加载字节码流，将静态结构（静态常量池）转换为运行时常量池，生成class对象**

**验证阶段验证安全确保不会危害到JVM，主要验证文件格式，类的元数据信息、字节码、符号引用等**

**准备阶段为类变量分配内存并默认初始化零值**

**解析阶段将常量池的符号引用替换为直接引用**

**初始化阶段执行类构造器（类变量赋值与类代码块的合并）**







## 深入浅出JVM（四）之类文件结构

Java文件编译成字节码文件后，通过类加载机制到Java虚拟机中，Java虚拟机能够执行所有符合要求的字节码，因此无论什么语言，只要能够编译成符合要求的字节码文件就能够被Java虚拟机执行

Java虚拟机和字节码是语言、平台无关性的基石

本篇文章将深入浅出的解析字节码文件


### 无关性的基石

曾经: 源代码->经过编译->本地机器码

Java: 源代码->经过编译->字节码 -> 解释器 -> 本地机器码

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727486996775474060.png)


**字节码: 与操作系统和机器指令集无关的,平台中立的程序编译后的存储格式**

> 字节码是无关性的基石

平台无关性的基石:

1.  **所有平台都统一支持字节码**
2.  **不同的Java虚拟机都可以执行平台无关的字节码**

因此实现了 **一次编译,到处运行**

语言无关性的基石:

1.  **Java虚拟机**
2.  **字节码**

Java虚拟机不是只可以执行Java源代码编译而成的字节码,只要符合要求(安全...)的字节码,它都可以执行

因此Kotlin...等语言可以运行在Java虚拟机上

### Class类文件结构

> 文件格式存取数据的类型

1.  无符号数 : u1,u2,u4,u8代表1,2,4,8个字节的无符号数(可以表示数字,UTF-8的字符串,索引引用....)
2.  表: 由n个无符号数或n个表组成(命名以`_info`结尾)

#### 初识Class文件格式

> 编写Java源代码

```java
 public class Test {
     private int m;
     private final int CONSTANT=111;
 
     public int inc() throws Exception {
         int x;
         try {
             x = 1;
             return x;
         }catch (Exception e){
             x = 2;
             return  x;
         }finally{
             x = 3;
         }
     }
 }
```

> 使用可视化工具classpy查看反编译的结果



 ![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487020613415581.png)

**每个集合前都有一个计数器来统计集合中元素的数量**

> Class文件格式的描述

| 数据类型       | 名称                | 数量                    | 对应图中名字     | 作用                                            |
| -------------- | ------------------- | ----------------------- | ---------------- | ----------------------------------------------- |
| u4             | magic               | 1                       | 魔数             | 确定这个文件是否是一个能被虚拟机接受的Class文件 |
| u2             | minor_version       | 1                       | 次版本号         | 虚拟机必须拒绝执行超过其版本号的Class文件       |
| u2             | major_version       | 1                       | 主版本号         | 虚拟机必须拒绝执行超过其版本号的Class文件       |
| u2             | constant_pool_count | 1                       | 常量池容量计数器 | 统计常量数量                                    |
| cp_info        | constant_pool       | constant_pool_count - 1 | 常量池           | 存放常量                                        |
| u2             | access_flags        | 1                       | 访问标志         | 识别类(类,接口)的访问信息                       |
| u2             | this_class          | 1                       | 类索引           | 确定类的全限定名                                |
| u2             | super_class         | 1                       | 父类索引         | 确定父类的全限定名                              |
| u2             | interfaces_count    | 1                       | 接口计数器       | 统计该类实现接口数量                            |
| u2             | interfaces          | interfaces_count        | 接口索引集合     | 描述该类实现了的接口                            |
| u2             | fields_count        | 1                       | 字段表集合计数器 | 统计类的字段数量                                |
| field_info     | fields              | fields_count            | 字段表集合       | 描述类声明的字段(类变量,实例变量)               |
| u2             | methods_count       | 1                       | 方法表集合计数器 | 统计类的方法数量                                |
| method_info    | methods             | methods_count           | 方法表集合       | 描述类声明的方法                                |
| u2             | attribute_count     | 1                       | 属性表集合计数器 | 统计属性数量                                    |
| attribute_info | attributes          | attributes_count        | 属性表集合       | 描述属性                                        |

#### 魔数与主次版本号

-   **魔数: 确定这个文件是否为一个能被虚拟机接受的有效Class文件**

-   **主次版本号: 虚拟机拒绝执行超过其版本号的Class文件**

    -   不同版本的Java前端编译器编译生成对应的Class文件主次版本号不同
    -   支持高版本JVM执行低版本前端编译器生成的Class文件(向下兼容)
    -   拒绝低版本JVM执行高版本前端编译器生成的Clsss文件

#### 常量池

常量池包含两大常量: **字面量和符号引用**

> 符号引用与直接引用

-   符号引用

    -   **使用一组符号描述引用**(为了定位到目标引用)
    -   **与虚拟机内存布局无关**
    -   **还是符号引用时目标引用不一定被加载到内存**

-   直接引用

    -   **直接执行目标的指针,相对偏移量或间接定位目标引用的句柄**
    -   **与虚拟机内存布局相关**
    -   **解析直接引用时目标引用已经被加载到内存中**

> 字面量与符号引用

-   **字面量**

    -   **文本字符串**
    -   **被final声明的常量**

-   **符号引用**

    -   **全限定名**
    -   **方法或字段的简单名称和描述符**


![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487046699763752.png)

图中的常量有我们代码中熟悉的常量也有很多没有显示出现在代码中的常量

#### 访问标志

**用于识别类或接口的访问信息**

是否是一个接口,枚举,模块,注解...

是否被final(public,abstract...)修饰

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487065257478652.png)

ACC_PUBLIC:被public修饰

ACC_SUPER: 允许使用invokespecial字节码指令

#### 类索引,父类索引与接口索引集合

> 类索引

**用于确定本类的全限定名**
![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487083312645388.png)



类索引指向常量池中表示该类的符号引用

> 父类索引

**用于确定父类的全限定名**

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487113439644040.png)


父类索引指向常量池中表示该类父类的符号引用

除了Object外,所有类的父类索引都不为0

> 接口索引集合

**描述这个类实现了哪些接口**

我们的例子中没有实现接口,就没有(接口索引集合计数器为0)

> 总结

**Class文件由 类索引,父类索引,接口索引集合 来确定该类的继承关系**

#### 字段表集合

**描述类声明的字段**

**字段包括类变量和成员变量(实例变量),不包括局部变量**


![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487130062374197.png)


> 简单名称和描述符

- **简单名称**

  -   **字段: 没有描述字段类型的名称**
  -   **方法: 没有描述参数列表和返回类型的名称**

- **描述符**

  - **字段: 描述字段的类型**

  - **方法: 描述参数列表和返回值**

  - **描述符字符含义(long,boolean,对象类型是J,Z,L 其他都是首字母大写)**

    | 标识字符 | 含义                         |
    | -------- | ---------------------------- |
    | B        | byte                         |
    | C        | char                         |
    | D        | double                       |
    | F        | float                        |
    | I        | int                          |
    | J        | long                         |
    | S        | short                        |
    | Z        | boolean                      |
    | V        | void                         |
    | L        | 对象类型,如Ljava/lang/Object |

  - 描述符描述n维数组

    - 在前面先写n个`[` 再写标识字符

      比如java.lang.Integer[ ] => `[Ljava.lang.Integer`

  - 描述符描述方法

    - 参数列表按照从左到右的顺序写在`()`中

    - 返回类型写到最后

      比如String method(long[],int,String[]) => `([JIL[java.lang.String)Ljava.lang.String`

因此Class文件中字段描述符指向常量池中的#07 I 符号引用(的索引)

> 注意

1.  **字段表集合不会列出父类或父接口中声明的字段**

2.  **只用 简单名称 来确定字段,所以不能有重名字段**

3.  **用 简单名称 和 描述符 确定方法,所以方法可以重名(重载)**

    -   字节码文件 规定 简单名称+描述符相同才是同一个方法
    -   但是 Java语法 规定 重载 = 简单名称相同 + 描述符的参数列表不同 + 描述符的返回类型不能不同

#### 方法表集合

**描述类声明的方法**

与字段表集合类似


![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487152603621444.png)


> 注意

**方法表集合中不会列出父类方法信息(不重写的情况)**

#### 属性表集合

属性比较多,这里只说明我们例子中出现的,其他的会总结

**用于描述某些场景专有信息**

刚刚在字段,方法表集合中都可以看到属性表集合,说明属性表集合是可以被携带的

> 怎么没看到Java源代码中的代码呢?

实际上它属于属性表集合中的Code属性

##### Code属性

**Java源代码中方法体中的代码经过编译后编程字节码指令存储在Code属性内**


![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487169903102438.png)


其中的异常表集合代表 **编译器为这段代码生成的多条异常记录,对应着可能出现的代码执行路径**

(程序在try中不抛出异常会怎么执行,抛出异常又会怎么执行....)

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487191177858699.png)


##### Exceptions属性

**列举出方法中可能抛出的检查异常(Checked Exception),也就是方法声明throws关键字后面的列举异常**

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487235806986016.png)



##### LineNumberTable属性

**描述Java源码行号与字节码指令行号(字节码偏移量)对应关系**

##### SourceFile属性

**记录生成此Class文件的源码名称**

##### StackMapTable属性

**虚拟机类加载验证阶段的字节码验证时,不需要再检验了,只需要查看StackMapTable属性中的记录是否合法**

**编译阶段将一系列的验证类型结果记录在StackMapTable属性中**


![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487249837222248.png)


##### ConstantValue

**在类加载的准备阶段,为静态变量(常量)赋值**

只有类变量才有这个属性

实例变量的赋值: 在实例构造器<init>

类变量的赋值: 在类构造器<clinit>或 带有ConstantValue属性在类加载的准备阶段

**如果类变量被final修饰(此时该变量是一个常量),且该变量数据类型是基本类型或字符串,就会生成ConstantValue属性,该属性指向常量池中要赋值的常量,在类加载的准备阶段,直接把在常量池中ConstantValue指向的常量赋值给该变量**



##### 总结所有属性

| 属性名                 | 作用                                                         |
| ---------------------- | ------------------------------------------------------------ |
| Code                   | 方法体内的代码经过编译后变为字节码指令存储在Code属性中       |
| Exceptions             | 列举出方法可能抛出的检查异常(Checked Exception)              |
| LineNumberTable        | Java源码行号与字节码偏移量(字节码行号)对应关系               |
| LocalVariableTable     | Java源码定义的局部变量与栈帧中局部变量表中的变量对应关系(*局部变量名称,描述符,局部变量槽位置,局部变量作用范围等*) |
| LocalVariableTypeTable | 与`LocalVariableTable`相似,只是把`LocalVariableTable`的描述符换成了字段的特征签名(完成对泛型的描述) |
| SourceFile             | 记录生成这个Class文件的源码文件名称                          |
| SourceDebugExtension   | 用于存储额外的代码调式信息                                   |
| ConstantValue          | 在类加载的准备阶段,为静态变量(常量)赋值                      |
| InnerClasses           | 记录内部类与宿主类之间的关系                                 |
| Deprecated             | 用于表示某个字段,方法或类已弃用 (可以用注解@deprecated表示)  |
| Synthetic              | 用于表示某字段或方法不是由Java源代码生成的,而是由编译器自行添加的 |
| StackMapTable          | 虚拟机类加载验证阶段的字节码验证时,不需要再检验了,只需要查看StackMapTable属性中的记录是否合法 |
| Signature              | 记录泛型签名信息                                             |
| BootstrapMethods       | 保存动态调用(invokeeddynamic)指令引用的引导方法限定符        |
| MethodParameters       | 记录方法的各个形参名称与信息                                 |

### javap解析Class文件

#### 关于javac

`javac xx.java` 编译Java源文件,不会生成对应的局部变量表

`javac -g xx.java` 编译Java源文件,生成对应的局部变量表

idea中编译Java源文件使用的是`javac -g`

#### 关于javap

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487321017482796.png)




> 常用

`javap -v` 基本上可以反汇编出Class文件中的很多信息(常量池,字段集合,方法集合...)

但是它不会显示私有字段或方法的信息,所以可以使用`javap -v -p`

> 详解javap -v -p

```
 public class JavapTest {
     private int a = 1;
     float b = 2.1F;
     protected double c = 3.5;
     public  int d = 10;
 
     private void test(int i){
         i+=1;
         System.out.println(i);
     }
 
     public void test1(){
         String s = "test1";
         System.out.println(s);
     }
 }
```

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487337515606477.png)


​    
![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487348433356638.png)


![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727487361076682686.png)



​    







## 深入浅出JVM（五）之Java中方法调用

本篇文章将围绕Java中方法的调用，深入浅出的说明方法调用的指令、解析调用以及分派调用等

### 方法调用

要知道Java中方法调用唯一目的就是确定要调用哪一个方法

方法调用可以分为**解析调用和分派调用**，接下来会详细介绍

 ![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728351798891413355.png)

#### 非虚方法与虚方法

**非虚方法: 静态方法，私有方法，父类中的方法，被final修饰的方法，实例构造器**

其他不是非虚方法的方法就是虚方法

非虚方法的特点就是没有重写方法，适合**在类加载阶段就进行解析(符号引用->直接引用)** 【编译时就能够确定】

#### 调用指令

- 普通调用指令

  -   **`invokestatic`:调用静态方法**
  -   **`invokespecial`:调用私有方法,父类中的方法,实例构造器<init>方法,final方法**
  -   **`invokeinterface`:调用接口方法**
  -   **`invokevirtual`: 调用虚方法**

  **使用`invokestatic`和`invokespecial`指令的一定是非虚方法**

  使用`invokeinterface`指令一定是虚方法(因为接口方法需要具体的实现类去实现)

  使用`invokevirtual`指令的是虚方法

- 动态调用指令

  -   `invokedynamic`: 动态解析出需要调用的方法再执行

  jdk 7 出现`invokedynamic`，支持动态语言

> 测试虚方法代码

-   父类

```java
 public class Father {
     public static void staticMethod(){
         System.out.println("father static method");
     }
 
     public final void finalMethod(){
         System.out.println("father final method");
     }
 
     public Father() {
         System.out.println("father init method");
     }
 
     public void overrideMethod(){
         System.out.println("father override method");
     }
 }
```

-   接口

```
 public interface TestInterfaceMethod {
     void testInterfaceMethod();
 }
```

-   子类

```java
 public class Son extends Father{
 
     public Son() {
         //invokespecial 调用父类init 非虚方法
         super();
         //invokestatic 调用父类静态方法 非虚方法
         staticMethod();
         //invokespecial 调用子类私有方法 特殊的非虚方法
         privateMethod();
         //invokevirtual 调用子类的重写方法 虚方法
         overrideMethod();
         //invokespecial 调用父类方法 非虚方法
         super.overrideMethod();
         //invokespecial 调用父类final方法 非虚方法
         super.finalMethod();
         //invokedynamic 动态生成接口的实现类 动态调用
         TestInterfaceMethod test = ()->{
             System.out.println("testInterfaceMethod");
         };
         //invokeinterface 调用接口方法 虚方法
         test.testInterfaceMethod();
     }
 
     @Override
     public void overrideMethod(){
         System.out.println("son override method");
     }
 
     private void privateMethod(){
         System.out.println("son private method");
     }
 
     public static void main(String[] args) {
         new Son();
     }
 }
```



 ![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728351817520754826.png)

**注意: 接口中的默认方法也是`invokeinterface`,接口中的静态方法是`invokestatic`**

#### 解析调用

解析调用就是在调用非虚方法

在编译期间就能够确定，运行时也不会改变

#### 分派调用

分派调用又分为静态分派与动态分配

**早期绑定:解析调用和静态分派这种编译期间可以确定调用哪个方法**

**晚期绑定: 动态分派这种编译期无法确定,要到运行时才能确定调用哪个方法**

##### 静态分派

```java
   //静态类型         实际类型
     List list = new ArrayList();
```

**静态分派: 根据静态类型决定方法执行的版本的分派**

**发生在编译期，特殊的解析调用**

典型的表现就是方法的重载

```java
 public class StaticDispatch {
     public void test(List list){
         System.out.println("list");
     }
 
     public void test(ArrayList arrayList){
         System.out.println("arrayList");
     }
 
     public static void main(String[] args) {
         ArrayList arrayList = new ArrayList();
         List list = new ArrayList();
         StaticDispatch staticDispatch = new StaticDispatch();
         staticDispatch.test(list);
         staticDispatch.test(arrayList);
     }
 }
 /*
 list
 arrayList
 */
```

方法的版本并不是唯一的,往往只能确定一个最适合的版本

##### 动态分派

**动态分派:动态期根据实际类型确定方法执行版本的分派**

动态分派与重写有着紧密的联系

```java
 public class DynamicDispatch {
     public static void main(String[] args) {
         Father father = new Father();
         Father son = new Son();
 
         father.hello();
         son.hello();
     }
     static class Father{
         public void hello(){
             System.out.println("Father hello");
         }
     }
 
     static class Son extends Father{
         @Override
         public void hello() {
             System.out.println("Son hello");
         }
     }
 }
 /*
 Father hello
 Son hello
 */
```



 ![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728351832191145684.png)

虽然常量池中的符号引用相同,`invokevirtual`指令最终指向的方法却不一样

> 分析invokevirtual指令搞懂它是如何确定调用的方法

1.  invokevirtual找到栈顶元素的*实际类型*
2.  如果在这个实际类型中找到与常量池中描述符与简单名称相符的方法，并通过访问权限的验证就返回这个方法的引用(未通过权限验证返回`IllegalAccessException`非法访问异常)
3.  如果在实际类型中未找到，就去实际类型的父类中寻找(没找到抛出`AbstractMethodError`异常)

因此，子类重写父类方法时，**根据invokevirtual指令规则，先在实际类型（子类）中寻找，找不到才去父类**，所以存在重写的多态

**频繁的动态分派会重新查找栈顶元素实际类型，会影响执行效率**

**为提高性能，JVM在该类方法区建立虚方法表使用索引表来代替查找**

> 字段不存在多态

当子类出现与父类相同的字段,子类会覆盖父类的字段

```java
 public class DynamicDispatch {
     public static void main(String[] args) {
         Father son = new Son();
     }
     static class Father{
         int num = 1;
 
         public Father() {
             hello();
         }
 
         public void hello(){
             System.out.println("Father hello " + num);
         }
     }
 
     static class Son extends Father{
         int num = 2;
 
         public Son() {
             hello();
         }
 
         @Override
         public void hello() {
             System.out.println("Son hello "+ num);
         }
     }
 }
 /*
 Son hello 0
 Son hello 2
 */
```

先对父类进行初始化，所以会先执行父类中的构造方法，而构造方法去执行了`hello()`方法，此时的实际类型是Son于是会去执行Son的hello方法，此时子类还未初始化成员变量，只是有个默认值，所以输出`Son hello 0`

##### 单分派与多分派

**方法参数或方法调用者被称为宗量**

分派还可以分为单、多分派

**根据一个宗量（方法参数或方法调用者）选择方法被称为单分派**

**根据多个宗量（方法参数和方法调用者）选择方法被称为多分派**

```java
 public class DynamicDispatch {
     public static void main(String[] args) {
         Father son = new Son();
         Father father = new Father();
 
         son.hello(new Nod());
         father.hello(new Wave());
     }
     static class Father{
 
 
         public void hello(Nod nod){
             System.out.println("Father nod hello " );
         }
 
         public void hello(Wave wave){
             System.out.println("Father wave hello " );
         }
     }
 
     static class Son extends Father{
 
         @Override
         public void hello(Nod nod) {
             System.out.println("Son nod hello");
         }
 
         @Override
         public void hello(Wave wave) {
             System.out.println("Son wave hello");
         }
     }
 
     //招手
     static class Wave{}
     //点头
     static class Nod{}
 }
 /*
 Son nod hello
 Father wave hello 
 */
```

在编译时，不仅要关心静态类型是Father还是Son，还要关心参数是Nod还是Wave，所以**静态分派是多分派(根据两个宗量对方法进行选择)**

在执行`son.hello(new Nod())`时只需要关心实际类型是Son还是Father，所以**动态分派是单分派(根据一个宗量对方法进行选择)**

### 总结

本篇文章围绕Java方法的调用，深入浅出的解析非虚方法与虚方法、调用的字节码指令、解析调用和分派调用、单分派以及多分派

**不能重写的方法（静态、私有、父类、final修饰、实例构造）被称为非虚方法，其他方法为虚方法**

**非虚方法是编译时就能够确定的，解析调用就是调用非虚方法**

**分派调用中的静态分派也是编译时确定的，是特殊的解析调用，根据静态类型选择方法，典型例子就是方法重载**

**分派调用中的动态分派是根据实际类型选择方法，在运行时才能够确定实际类型，典型例子就是方法重写**

**静态分派需要考虑方法调用者和方法参数是多分派，动态分派只需要考虑方法调用者是单分派**









## 深入浅出JVM（六）之前端编译过程与语法糖原理

本篇文章将围绕Java中的编译器，深入浅出的解析前端编译的流程、泛型、条件编译、增强for循环、可变长参数、lambda表达式等语法糖原理

### 编译器

Java中的编译器不止一种，Java编译器可以分为：**前端编译器、即时编译器和提前编译器**

最为常见的就是**前端编译器javac，它能够将Java源代码编译为字节码文件，它能够优化程序员使用起来很方便的语法糖**

**即时编译器是在运行时，将热点代码直接编译为本地机器码，而不需要解释执行，提升性能**

**提前编译器将程序提前编译成本地二进制代码**



### 前端编译过程

- 准备阶段: 初始化插入式注解处理器

- 处理阶段

  - 解析与填充符号表

    1.  **词法分析: 将Java源代码的字符流转变为token(标记)流**

        -   字符: 程序编写的最小单位
        -   标记(token) : 编译的最小单位
        -   比如 关键字 static 是一个标记 / 6个字符

    2.  **语法分析: 将token流构造成抽象语法树**

    3.  **填充符号表: 产生符号信息和符号地址**

        -   符号表是一组符号信息和符号地址构成的数据结构
        -   比如: 目标代码生成阶段，对符号名分配地址时，要查看符号表上该符号名对应的符号地址

  - 插入式注解处理器的注解处理

    4.  **注解处理器处理特殊注解: 在编译器允许注解处理器对源代码中特殊注解作处理，可以读写抽象语法树中任意元素，如果发生了写操作，就要重新解析填充符号表**

        -   比如: Lombok通过特殊注解，生成get/set/构造器等方法

  - 语义分析与字节码生成

    5. **标注检查: 对语义静态信息的检查以及常量折叠优化**

       ```java
        int i = 1;
        char c1 = 'a';
        int i2 = 1 + 2;//编译成 int i2 = 3 常量折叠优化
        char c2 = i + c1; //编译错误 标注检查 检查语法静态信息 
       ```

        ![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728377993608176384.png)

    6. **数据及控制流分析: 对程序运行时动态检查**

       -   比如方法中流程控制产生的各条路是否有合适的返回值

    7. **解语法糖: 将(方便程序员使用的简洁代码)语法糖转换为原始结构**

    8. **字节码生成: 生成`<init>,<clinit>`方法,并根据上述信息生成字节码文件**

> 前端编译流程图


![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378021049959775.png)

> 源码分析

![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378036672153223.png)


代码位置在JavaCompiler的compile方法中

![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378246290725735.png)




### Java中的语法糖

#### 泛型

**将操作的数据类型指定为方法签名中一种特殊参数，作用在方法、类、接口上时称为泛型方法、泛型类、泛型接口**

Java中的泛型是**类型擦除式泛型**，泛型只在源代码中存在，**在编译期擦除泛型，并在相应的地方加上强制转换代码**

> 与具现化式泛型(不会擦除，运行时也存在泛型)对比

- 优点: 只需要改动编译器，Java虚拟机和字节码指令不需要改变

  -   因为泛型是JDK5加入的，为了满足对以前版本代码的兼容采用类型擦除式泛型

- 缺点: 性能较低，使用没那么方便

  - 为提供基本类型的泛型，只能自动拆装箱，在相应的地方还会加速强制转换代码，所以性能较低

  - 运行期间无法获取到泛型类型信息

    - 比如书写泛型的List转数组类型时，需要在方法的参数中指定泛型类型

      ```java
       public static <T> T[] listToArray(List<T> list,Class<T> componentType){
               T[] instance = (T[]) Array.newInstance(componentType, list.size());
               return instance;
       }
      ```

#### 增强for循环与可变长参数


![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378281334653278.png)

增强for循环 -> 迭代器

可变长参数 -> 数组装载参数

泛型擦除后会在某些位置插入强制转换代码

#### 自动拆装箱

> 自动装箱、拆箱的错误用法

```java
         Integer a = 1;
         Integer b = 2;
         Integer c = 3;
         Integer d = 3;
         Integer e = 321;
         Integer f = 321;
         Long g = 3L;
         //true
         System.out.println(c == d);//范围小，在缓冲池中
         //false
         System.out.println(e == f);//范围大，不在缓冲池中，比较地址因此为false
         //true
         System.out.println(c == (a + b));
         //true
         System.out.println(c.equals(a + b));
         //false
         System.out.println(g == (b + a));
         //true
         System.out.println(g.equals(a + b));
```

-   注意:

    1.  包装类重写的equals方法中不会自动转换类型
        ![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378311992979938.png)

    2.  包装类的 == 就是去比较引用地址，不会自动拆箱

#### 条件编译

布尔类型 + if语句 : **根据布尔值类型的真假，编译器会把分支中不成立的代码块消除（解语法糖）**


![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378333265864400.png)

#### Lambda原理

> 编写函数式接口

```java
 @FunctionalInterface
 interface LambdaTest {
     void lambda();
 }
```

> 编写测试类

```java
 public class Lambda {
     private int i = 10;
 
     public static void main(String[] args) {
         test(() -> System.out.println("匿名内部类实现函数式接口"));
     }
 
     public static void test(LambdaTest lambdaTest) {
         lambdaTest.lambda();
     }
 }
```

> 使用插件查看字节码文件

![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378358310444617.png)
生成了一个私有静态的方法,这个方法中很明显就是lambda中的代码

**在使用lambda表达式的类中隐式生成一个静态私有的方法，这个方法代码块就是lambda表达式中写的代码**

![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378466502117793.png)
执行class文件时带上参数`java -Djdk.internal.lambda.dumpProxyClasses 包名.类名`即可显示出这个匿名内部类


 ![](https://bbs-img.huaweicloud.com/blogs/img/20241008/1728378493845688313.png)

**使用`invokedynamic`生成了一个实现函数式接口的匿名内部类对象，在重写函数式接口的方法实现中调用使用lambda表达式类中隐式生成的静态私有方法**

### 总结

本篇文章以Java中编译器的分类为开篇，深入浅出的解析前端编译的流程，Java中泛型、增强for循环、可变长参数、自动拆装箱、条件编译以及Lambda等语法糖的原理

**前端编译先将字符流转换为token流，再将token流转换为抽象语法树，填充符号表的符号信息、符号地址，然后注解处理器处理特殊注解（比如Lombok生成get、set方法），对语法树发生写改动则要重新解析、填充符号，接着检查语义静态信息以及常量折叠，对运行时程序进行动态检查，再解语法糖，生成init实例方法、clinit静态方法，最后生成字节码文件**

**Java中为了兼容之前的版本使用类型擦除式的泛型，在编译期间擦除泛型并在相应位置加上强制转换，想为基本类型使用泛型只能搭配自动拆装箱一起使用，性能有损耗且在运行时无法获取泛型类型**

**增加for循环则是使用迭代器实现，并在适当位置插入强制转换；可变长参数则是创建数组进行装载参数**

**自动拆装箱提供基本类型与包装类的转换，但包装类尽量不使用==，这是去比较引用地址，同类型比较使用equals**

**条件编译会在if-else语句中根据布尔类型将不成立的分支代码块消除**

**lambda原理则是通过`invokeDynamic`指令动态生成实现函数式接口的匿名对象，匿名对象重写函数时接口方法中调用使用lambda表达式类中隐式生成的静态私有的方法（该方法就是lambda表达式中的代码内容）**





## 深入浅出JVM（七）之执行引擎的解释执行与编译执行


本篇文章围绕执行引擎，**深入浅出的解析执行引擎中解释器与编译器的解释执行和编译执行、执行引擎的执行方式、逃逸分析带来的栈上分配、锁消除、标量替换等优化以及即时编译器编译对热点代码的探测**

### 执行引擎

> hotspot执行引擎结构图

执行引擎分为解释器、JIT即时编译器以及垃圾收集器

**执行引擎通过解释器/即时编译器将字节码指令解释/编译为对应OS上的的机器指令**

 ![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728522472668187478.png)


本篇文章主要围绕解释器与即时编译器，垃圾收集器将在后续文章解析

### 解释执行与编译执行

Java虚拟机执行引擎在执行Java代码时，会有两种选择:*解释执行和编译执行*

解释执行：通过**字节码解释器把字节码解析为机器语言**执行

编译执行：通过**即时编译器产生本地代码**执行

Class文件中的代码到底是解释执行还是编译执行只有Java虚拟机自己才能判断准确

> 编译过程

![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728522483439334620.png)



编译流程在前一篇文章[深入浅出JVM之前端编译过程与语法糖原理](https://juejin.cn/post/7177311689916825657)已经说明，在本篇文章中不再概述

经典编译原理: **1.对源码进行词法，语法分析处理 2.把源码转换为抽象语法树**

**javac编译器完成了对源码进行词法，语法分析处理为抽象语法树，再遍历抽象语法树生成线性字节码指令流的过程**

剩下的指令流有两种方式执行

1.  由虚拟机内部的字节码解释器去将字节码指令进行逐行解释 （解释执行）
2.  或优化器（即时编译器）优化代码最后生成目标代码 （编译执行）

> 执行引擎流程图

![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728522500133455773.png)



### 解释器与编译器

> 解释器

**作用: 对字节码指令逐行解释**

优点: 程序启动，解释器立即解释执行

缺点: 低效

> 即时编译器 (just in time compiler)

Java中的"编译期"不确定

-   可能说的是执行javac指令时的前端编译器 (.java->.class)
-   也可能是后端编译器JIT (字节指令->机器指令)
-   还可能是AOT编译器(静态提前编译器) (.java->机器指令)

**作用: 将方法编译成机器码缓存到方法区，每次调用该方法执行编译后的机器码**

优点: 即时编译器把代码编译成本地机器码，执行效率高，高效

缺点: 程序启动时，需要先编译再执行

### 执行引擎执行方式

执行引擎执行方式大致分为3种

`-Xint`: 完全采用解释器执行

`-Xcomp`: 优先采用即时编译器执行，解释器是后备选择

`-Xmixed`: 采用解释器 + 即时编译器

![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728522517652195813.png)


hotspot中有两种JIT即时编译器

Client模式下的**C1编译器：简单优化，耗时短**（C1优化策略：方法内联，去虚拟化，冗余消除）

Server模式下的**C2编译器：深度优化，耗时长** （C2主要是逃逸分析的优化：标量替换，锁消除，栈上分配）

**分层编译策略：程序解释执行（不开启逃逸分析）可以触发C1编译，开启逃逸分析可以触发C2编译**

**解释器，C1，C2编译器同时工作，热点代码可能被编译多次**

解释器在程序刚刚开始的时候解释执行，不需要承担监控的开销

C1有着更快的编译速度，能为C2编译优化争取更多时间

C2用高复杂度算法，编译优化程度很高的代码

### 逃逸分析带来的优化

**当对象作用域只在某个方法时，不会被外界调用到**，那么这个对象就不会发生逃逸

开启逃逸分析后，会分析对象是否发生逃逸，**当不能发生逃逸时会进行栈上分配、锁消除、标量替换等优化**

#### 栈上分配内存

```java
 //-Xms1G -Xmx1G -XX:+PrintGCDetails 
 public class StackMemory {
     public static void main(String[] args) {
         long start = System.currentTimeMillis();
 
         for (int i = 0; i < 10000000; i++) {
             memory();
         }
 
         System.out.println("花费时间:"+(System.currentTimeMillis()-start)+"ms");
 
         try {
             TimeUnit.SECONDS.sleep(1000);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
 
     private static void memory(){
         StackMemory memory = new StackMemory();
     }
 }
```

-XX:-DoEscapeAnalysis 花费时间:63ms (未开启逃逸分析)

-XX:+DoEscapeAnalysis 花费时间:4ms (开启逃逸分析)

**默认开启逃逸分析**

#### 锁消除

同步加锁会带来开销

锁消除：当加锁对象只作用某个方法时，JIT编译器借助逃逸分析**判断使用的锁对象是不是只能被一个线程访问**，如果是这种情况下就不需要同步，可以取消这部分代码的同步，提高并发性能

#### 标量替换

标量: 无法再分解的数据 (基本数据类型)

聚合量: 还可以再分解的数据 (对象)

**标量替换: JIT借助逃逸分析，该对象不发生逃逸，只作用于某个方法会把该对象(聚合量)拆成若干个成员变量(标量)来代替**

**默认开启标量替换**

```java
 public class ScalarSubstitution {
     static class Man{
         int age;
         int id;
 
         public Man() {
         }
     }
 
     public static void createInstance(){
         Man man = new Man();
         man.id = 123;
         man.age = 321;
     }
     public static void main(String[] args) {
         long start = System.currentTimeMillis();
 
         for (int i = 0; i < 10000000; i++) {
             createInstance();
         }
 
         System.out.println("花费时间:"+(System.currentTimeMillis()-start)+"ms");
 
         try {
             TimeUnit.SECONDS.sleep(1000);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
 }
```

```
 //-Xmx200m -Xms200m -XX:+PrintGCDetails 
 //-XX:+DoEscapeAnalysis 设置开启逃逸分析
 //-XX:-EliminateAllocations 设置不开启标量替换 
 //开启逃逸分析 + 关闭标量替换 : 花费时间:93ms
 //开启逃逸分析 + 开启标量替换  : 花费时间:6ms
```


### 热点代码与热点探测

JIT编译器并不是编译所有的字节码，JIT编译器只编译热点代码

**热点代码: 被多次调用的方法 或 方法中多次循环的循环体**

**栈上替换(OSR): JIT将方法中的热点代码编译为本地机器指令（被多次执行的循环体）**

**编译对象都是方法，如果是栈上替换则"入口"在方法的循环体开始那里**

**热点探测功能**决定了被调用多少次的方法能成为热点代码

hotspot采用**基于计数器的热点探测**

-   **方法调用计数器 : 统计方法调用次数**
-   **回边计数器 : 统计循环体执行循环次数**

方法调用时先判断是否有执行编译后的机器码，有则直接使用方法区的Code cache中的机器码；没有机器码则判断计数器次数是否超过阈值，超过则触发编译，编译后机器码存储在方法区Code cache中使用；最后都没有就使用解释执行



### 总结

本篇文章将围绕执行引擎，深入浅出的解析执行引擎中的解释器、即时编译器各自执行的优缺点以及原理

**执行引擎由解释器、即时编译器、垃圾收集器构成，默认情况下使用解释器与编译器的混合方式执行**

**即时编译器分为C1、C2编译器，其中C1编译快但优化小，C2开启逃逸分析使用栈上分配、锁消除、标量替换进行优化，编译耗时但是优化大**

**即时编译器并不是所有代码都编译，而是使用方法技术和循环计数来将热点代码编译成机器码存放在方法区的Code Cache中**

**在混合执行的模式下，解释器、C1、C2编译器同时工作，分层编译**















## 深入浅出JVM（八）之类加载器

前文已经描述Java源文件经过前端编译器后变成字节码文件，字节码文件通过类加载器的类加载机制在Java虚拟机中生成Class对象

前文[深入浅出JVM（六）之前端编译过程与语法糖原理](https://juejin.cn/post/7177311689916825657)重点描述过编译的过程

前文[深入浅出JVM（三）之HotSpot虚拟机类加载机制](https://juejin.cn/post/7169887169828356110)重点描述过类加载机制的过程

本篇文章将重点聊聊类加载器，**围绕类加载器深入浅出的解析类加载器的分类、种类、双亲委派模型以及从源码方面推导出我们的结论**

#### 类加载器简介

> 什么是类加载器?

类加载器通过类的全限定类名进行类加载机制从而生成Class对象

Class对象中包含该类相关类信息，通过Class对象能够使用反射在运行时阶段动态做一些事情

> 显示加载与隐式加载

类加载器有两种方式进行加载，一种是**在代码层面显示的调用**，另一种是**当程序遇到创建对象等命令时自行判断该类是否进行过加载，未加载就先进行类加载**

显示加载：显示调用ClassLoader加载class对象

隐式加载：不显示调用ClassLoader加载class对象(因为虚拟机会在第一次使用到某个类时自动加载这个类)

```java
 //显示类加载  第7章虚拟机类加载机制.User为全限定类名(包名+类名)
 Class.forName("第7章虚拟机类加载机制.User");
             
 //隐式类加载
 new User();    
```

> 唯一性与命名空间

判断两个类是否完全相同可能并不是我们自认为理解的那样，类在JVM中的唯一性需要根据类本身和加载它的类加载器

-   唯一性

    -   **所有类都由它本身和加载它的那个类在JVM中确定唯一性**
    -   也就是说判断俩个类是否为同一个类时，如果它们的类加载器都不同那肯定不是同一个类

-   命名空间

    -   **每个类加载有自己的命名空间，命名空间由所有父类加载器和该加载器所加载的类组成**
    -   同一命名空间中，不存在类完整名相同的俩个类
    -   不同命名空间中，允许存在类完整名相同的俩个类（多个自定义类加载加载同一个类时，会在各个类加载器中生成对应的命名，且它们都不是同一个类）

> 基本特征

类加载器中有一些基本特性，比如子类加载器可以访问父类加载器所加载的类、父类加载过的类子类不再加载、双亲委派模型等

-   可见性

    -   子类加载器可以访问父类加载器所加载的类*
    -   (命名空间包含父类加载器加载的类)

-   单一性

    -   因为可见性，所以父类加载器加载过的类，子类加载器不会再加载
    -   同一级的自定义类加载器可能都会加载同一个类，因为它们互不可见

-   双亲委派模型

    -   由哪个类加载器来进行类加载的一套策略，后续会详细说明

#### 类加载器分类

类加载器可以分成两种，一种是引导类由非Java语言实现的，另一种是由Java语言实现的自定义类加载器

-   引导类加载器 (c/c++写的Bootstrap ClassLoader)
-   自定义类加载器：由`ClassLoader`类派生的类加载器类(包括扩展类，系统类，程序员自定义加载器等)


![image-20210425232253366.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/61da233f7ea849b88457de1e4d58cba3~tplv-k3u1fbpfcp-watermark.image?)
系统（应用程序）类加载器和扩展类加载器是Launcher的内部类，它们间接实现了`ClassLoader`

> 注意



![image-20210516203457475.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/207bb4e383cd4343b2deb6d88120498c~tplv-k3u1fbpfcp-watermark.image?)
平常说的系统（应用程序）类加载器的父类加载器是扩展类加载器，而扩展类加载器的父类加载器是启动类加载器，都是"逻辑"上的父类加载器

实际上扩展类加载器和系统（应用程序）类加载器间接继承的`ClassLoader`中有一个字段`parent`用来表示自己的逻辑父类加载器

#### 类加载器种类

-   启动（引导）类加载器

    -   Bootstrap Classloader c++编写，无法直接获取
    -   **加载核心库`<JAVA_HOME>\lib\部分jar包`**
    -   不继承`java.lang.ClassLoader`，没有父类加载器
    -   加载扩展类加载器和应用程序类加载器，并指定为它们的父类加载器

-   扩展类加载器

    -   Extension Classloader
    -   **加载扩展库`<JAVA_HOME>\lib\ext*.jar`**
    -   间接继承`java.lang.ClassLoader`，父类加载器为启动类加载器

-   应用程序(系统)类加载器

    -   App(System) Classloader 最常用的加载器
    -   **负责加载环境变量classpath或java.class.path指定路径下的类库 ，一般加载我们程序中自定义的类**
    -   间接继承`java.lang.ClassLoader`，父类加载器为扩展类加载器
    -   使用`ClassLoader.getSystemClassLoader()`获得

-   **自定义类加载器(实现ClassLoader类，重写findClass方法)**

通过代码来演示:

```java
 public class TestClassLoader {
     public static void main(String[] args) {
         URL[] urLs = Launcher.getBootstrapClassPath().getURLs();
         /*
         启动类加载器能加载的api路径:
         file:/D:/Environment/jdk1.8.0_191/jre/lib/resources.jar
         file:/D:/Environment/jdk1.8.0_191/jre/lib/rt.jar
         file:/D:/Environment/jdk1.8.0_191/jre/lib/sunrsasign.jar
         file:/D:/Environment/jdk1.8.0_191/jre/lib/jsse.jar
         file:/D:/Environment/jdk1.8.0_191/jre/lib/jce.jar
         file:/D:/Environment/jdk1.8.0_191/jre/lib/charsets.jar
         file:/D:/Environment/jdk1.8.0_191/jre/lib/jfr.jar
         file:/D:/Environment/jdk1.8.0_191/jre/classes
         */
         System.out.println("启动类加载器能加载的api路径:");
         for (URL urL : urLs) {
             System.out.println(urL);
         } 
 
         /*
         扩展类加载器能加载的api路径:
         D:\Environment\jdk1.8.0_191\jre\lib\ext;C:\WINDOWS\Sun\Java\lib\ext
         */
         System.out.println("扩展类加载器能加载的api路径:");
         String property = System.getProperty("java.ext.dirs");
         System.out.println(property);
         
         //加载我们自定义类的类加载器是AppClassLoader,它是Launcher的内部类
         ClassLoader appClassLoader = TestClassLoader.class.getClassLoader();
         //sun.misc.Launcher$AppClassLoader@18b4aac2 
         System.out.println(appClassLoader);
         
         //AppClassLoader的上一层加载器是ExtClassLoader,它也是Launcher的内部类
         ClassLoader extClassloader = appClassLoader.getParent();
         //sun.misc.Launcher$ExtClassLoader@511d50c0
         System.out.println(extClassloader);
         
         //实际上是启动类加载器,因为它是c/c++写的,所以显示null
         ClassLoader bootClassloader = extClassloader.getParent();
         //null 
         System.out.println(bootClassloader);
         
         //1号测试：基本类型数组 的类加载器
         int[] ints = new int[10];
         //null 
         System.out.println(ints.getClass().getClassLoader());
         
         //2号测试：系统提供的引用类型数组 的类加载器
         String[] strings = new String[10];
         //null 
         System.out.println(strings.getClass().getClassLoader());
         
         //3号测试：自定义引用类型数组 的类加载器
         TestClassLoader[] testClassLoaderArray = new TestClassLoader[10];
         //sun.misc.Launcher$AppClassLoader@18b4aac2       
         System.out.println(testClassLoaderArray.getClass().getClassLoader());
 
         //4号测试：线程上下文的类加载器
         //sun.misc.Launcher$AppClassLoader@18b4aac2
         System.out.println(Thread.currentThread().getContextClassLoader());
     }
 }
```

从上面可以得出结论

1.  **数组类型的类加载器是数组元素的类加载器**（通过2号测试与3号测试的对比）
1.  **基本类型不需要类加载** （通过1号测试与3号测试的对比）
1.  **线程上下文类加载器是系统类加载器** （通过4号测试）

#### 关于类加载源码解析

##### 用源码来解释上文结论

-   `ClassLoader`中的官方注释


![image-20210516222159652.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3f2e3c386088428aa7ee2f81f0d692ef~tplv-k3u1fbpfcp-watermark.image?)

   **虚拟机自动生成的一个类，管理数组，会对这个类进行类加载**

   **对数组类类加载器是数组元素的类加载器**

   **如果数组元素是基本类型则不会有类加载器**

- 源码解释扩展类加载器的父类是null

    

![image-20210516221509952.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/442bf71bf74b420da1fd470e5c17df3c~tplv-k3u1fbpfcp-watermark.image?)

- 源码解释系统类加载器的父类是扩展类加载器

    

![image-20210516214113943.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9e7426f1c6f044abbf7d68bb5ec24456~tplv-k3u1fbpfcp-watermark.image?)

- 源码解释线程上下文类加载器是系统类加载器

    

![image-20210516221726540.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/fe8e7a56b9524c72a23ed8f81f322a43~tplv-k3u1fbpfcp-watermark.image?)

##### ClassLoader主要方法

> loadClass()

`ClassLoader`的 `loadClass`方法（双亲委派模型的源码）

```java
 public Class<?> loadClass(String name) throws ClassNotFoundException {
     return loadClass(name, false);
 }
```

```java
                                             //参数resolve:是否要解析类
 protected Class<?> loadClass(String name, boolean resolve)
             throws ClassNotFoundException
     {
        //加锁同步 保证只加载一次
         synchronized (getClassLoadingLock(name)) {
             // 首先检查这个class是否已经加载过了
             Class<?> c = findLoadedClass(name);
             if (c == null) {
                 long t0 = System.nanoTime();
                 try {
                     // c==null表示没有加载，如果有父类的加载器则让父类加载器加载
                     if (parent != null) {
                         c = parent.loadClass(name, false);
                     } else {
                         //如果父类的加载器为空 则说明递归到bootStrapClassloader了
                         //则委托给BootStrap加载器加载
                         //bootStrapClassloader比较特殊无法通过get获取
                         c = findBootstrapClassOrNull(name);
                     }
                 } catch (ClassNotFoundException e) {
                     //父类无法加载抛出异常
                 }
                 //如果父类加载器仍然没有加载过，则尝试自己去加载class
                 if (c == null) {
                     long t1 = System.nanoTime();
                     c = findClass(name);
                     sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                     sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                     sun.misc.PerfCounter.getFindClasses().increment();
                 }
             }
             //是否要解析
             if (resolve) {
                 resolveClass(c);
             }
             return c;
         }
 }
```

**先递归交给父类加载器去加载，父类加载器未加载再由自己加载**

> findClass()

`ClassLoader`的`findClass()`

```java
     protected Class<?> findClass(String name) throws ClassNotFoundException {
         throw new ClassNotFoundException(name);
     }
```

由子类`URLClassLoader`重写findClass去寻找类的规则


![image-20210516225918213.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0964cac1bb6148d58fc9348d21638dbe~tplv-k3u1fbpfcp-watermark.image?)

最后都会来到`defineClass()`方法

> defineClass()

```java
 protected final Class<?> defineClass(String name, byte[] b, int off, int len)
```

根据从off开始长度为len定字节数组b转换为Class实例

**在自定义类加载器时，覆盖`findClass()`编写加载规则，取得要加载的类的字节码后转换为流调用`defineClass()`生成Class对象**

> resolveClass()

```java
     protected final void resolveClass(Class<?> c) {
         resolveClass0(c);
     }
```

使用该方法可以在生成Class对象后，解析类(符号引用 -> 直接引用)

> findLoadedClass()

```java
     protected final Class<?> findLoadedClass(String name) {
         if (!checkName(name))
             return null;
         return findLoadedClass0(name);
     }
```

如果加载过某个类则返回Class对象否则返回null

##### Class.forName()与ClassLoader.loadClass()区别

-   Class.forName()

    -   传入一个类的全限定名返回一个Class对象
    -   **将Class文件加载到内存时会初始化，主动引用**

-   ClassLoader.loadClass()

    -   需要class loader对象调用
    -   通过上面的源码分析可以知道，双亲委派模型调用loadClass，**只是将Class文件加载到内存，不会初始化和解析，直到这个类第一次使用才进行初始化**

#### 双亲委派模型

双亲委派模型源码实现对应`ClassLoader`的`loadClass()`

- 分析：

  1. 先检查这个类是否加载过

  1. 没有加载过，查看父类加载器是否为空，

     如果不为空，就交给父类加载器去加载（递归），

     如果为空，说明已经到启动类加载器了（启动类加载器不能get因为是c++写的）

  1. 如果父类加载器没有加载过，则递归回来自己加载

- 举例

  1. 假如我现在自己定义一个MyString类，它会自己找（先在系统类加载器中找，然后在扩展类加载器中找，最后去启动类加载器中找，启动类加载器无法加载然后退回扩展类加载器，扩展类加载器无法加载然后退回系统类加载器，然后系统类加载器就完成加载）

  1. 我们都知道Java有java.lang.String这个类

     那我再创建一个java.lang.String运行时，报错
     ![image-20200328155339760.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/673953b04774442ca58c27adac5c380f~tplv-k3u1fbpfcp-watermark.image?)


可是我明明写了main方法

这是因为**类装载器的双亲委派模型**

很明显这里的报错是因为它找到的是启动类加载器中的java.lang.String而不是在应用程序类加载器中的java.lang.String(我们写的)

而且核心类库的包名也是被禁止使用的 

![image-20210425231816778.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bd8cd5fdea494884b157319b5009c28e~tplv-k3u1fbpfcp-watermark.image?)

**类装载器的加载机制：启动类加载器->扩展类加载器->应用程序类加载器**

3. 如果自定义类加载器重写`loadClass`不使用双亲委派模型是否就能够用自定义类加载器加载核心类库了呢?

   **JDK为核心类库提供一层保护机制,不管用什么类加载器最终都会调用`defineClass()`,该方法会执行`preDefineClass()`，它提供对JDK核心类库的保护**  


![image-20210517103634644.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c0935748d5f048e291bdb692b8ff04f6~tplv-k3u1fbpfcp-watermark.image?)  

-   优点

    1.  **防止重复加载同一个class文件**
    1.  **保证核心类不能被篡改**

-   缺点

    -   **父类加载器无法访问子类加载器**

        -   比如系统类中提供一个接口，实现这个接口的实现类需要在系统类加载器加载，而该接口提供静态工厂方法用于返回接口的实现类的实例，但由于启动类加载器无法访问系统类加载器，这时静态工厂方法就无法创建由系统类加载器加载的实例

-   Java虚拟机规范只是建议使用双亲委派模型，不是一定要使用

    -   Tomcat中是由自己先去加载，加载失败再由父类加载器去加载

#### 自定义类加载器

1.  继承`ClassLoader`类

1.  可以覆写`loadClass`方法，也可以覆写`findClass`方法

    -   建议覆写`findClass`方法，因为loadClass是双亲委派模型实现的方法，其中父类类加载器加载不到时会调用`findClass`尝试自己加载

1.  编写好后调用`loadClass`方法来实现类加载

> 自定义类加载器代码

``` java
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
        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            int len = -1;
            byte[] bytes = new byte[1024];
            while ((len = bis.read(bytes)) != -1) {
                baos.write(bytes,0,len);
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
```

> 客户端调用自定义类加载器加载类

``` java
public class Client {
    public static void main(String[] args) {
        MyClassLoader myClassLoader = new MyClassLoader("C:\");
        try {
            Class<?> classLoader = myClassLoader.loadClass("HotTest");
            System.out.println("类加载器为:" + classLoader.getClassLoader().getClass().getName());
            System.out.println("父类加载器为" + classLoader.getClassLoader().getParent().getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
```

记得对要加载的类先进行编译


![image-20210517123408569.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/870f0393e7f440eba6076afde3b5362e~tplv-k3u1fbpfcp-watermark.image?)

-   注意:

    -   要加载的类不要放在父类加载器可以加载的目录下
    -   自定义类加载器父类加载器为系统类加载器
    -   JVM所有类类加载都使用loadClass

> 解释如果类加载器不同那么它们肯定不是同一个类

```java
	MyClassLoader myClassLoader1 = new MyClassLoader("D:\代码\JavaVirtualMachineHotSpot\src\main\java\");
        MyClassLoader myClassLoader2 = new MyClassLoader("D:\代码\JavaVirtualMachineHotSpot\src\main\java\");
        try {
            Class<?> aClass1 = myClassLoader1.findClass("HotTest");
            Class<?> aClass2 = myClassLoader2.findClass("HotTest");
            System.out.println(aClass1 == aClass2);//false
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
```

-   优点

    -   隔离加载类 (各个中间件jar包中类名可能相同,但自定义类加载器不同)
    -   修改类加载方式
    -   扩展加载源 (可以从网络,数据库中进行加载)
    -   防止源码泄漏 (Java反编译容易,可以编译时进行加密,自定义类加载解码字节码)

#### 热替换

**热替换: 服务不中断，修改会立即表现在运行的系统上**

对Java来说，如果一个类被类加载器加载过了，就无法被再加载了

但是如果每次加载这个类的类加载不同，那么就可以实现热替换

还是使用上面写好的自定义类加载器

```java
        //测试热替换
        try {
            while (true){
                MyClassLoader myClassLoader = new MyClassLoader("D:\代码\JavaVirtualMachineHotSpot\src\main\java\");
                
                Class<?> aClass = myClassLoader.findClass("HotTest");
                Method hot = aClass.getMethod("hot");
                Object instance = aClass.newInstance();
                Object invoke = hot.invoke(instance);
                TimeUnit.SECONDS.sleep(3);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
```

通过反射调用HotTest类的hot方法

中途修改hot方法并重新编译


![image-20210517124720782.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/60d4366a35514a85a24705dabf983575~tplv-k3u1fbpfcp-watermark.image?)

### 总结

本篇文章围绕类加载器深入浅出的解析类加载器的分类与种类、双亲委派模型、通过源码解析证实我们的观点、最后还自定义的类加载器和说明热替换

**类加载器将字节码文件进行类加载机制生成Class对象从而加载到Java虚拟机中**

**类加载只会进行一次，能够显示调用执行或者在遇到创建对象的字节码命令时隐式判断是否进行过类加载**

**类加载器分为非Java语言实现的引导类加载器和Java语言实现的自定义类加载器，其中JDK中实现了自定义类加载器中的扩展类加载器和系统类加载器**

**引导类加载器用来加载Java的核心类库，它的子类扩展类加载器用来加载扩展类，扩展类的子类系统类加载器常用于加载程序中自定义的类（这里的父子类是逻辑的，并不是代码层面的继承）**

**双亲委派模型让父类加载器优先进行加载，无法加载再交给子类加载器进行加载；通过双亲委派模型和沙箱安全机制来保护核心类库不被其他恶意代码替代**

**基本类型不需要类加载、数组类型的类加载器是数组元素的类加载器、线程上下文类加载器是系统类加载器**

**由于类和类加载器才能确定JVM中的唯一性，每次加载类的类加载不同时就能够多次进行类加载从而实现在运行时修改的热替换**





















## 深入浅出JVM（九、十）之字节码指令

本篇文章主要围绕字节码的指令，深入浅出的解析各种类型字节码指令，如：加载存储、算术、类型转换、对象创建与访问、方法调用与返回、控制转义、异常处理、同步等

由于字节码指令种类太多，本文作为上篇概述加载存储、算术、类型转换的字节码指令

使用idea中的插件jclasslib查看编译后的字节码指令

大部分指令先**以i(int)、l(long)、f(float)、d(double)、a(引用)开头**

**其中byte、char、short、boolean在hotspot中都是转成int去执行(使用int类型的字节码指令)**

字节码指令大致分为:

1.  **加载与存储指令**
2.  **算术指令**
3.  **类型转换指令**
4.  **对象创建与访问指令**
5.  **方法调用与返回指令**
6.  **操作数栈管理指令**
7.  **控制转义指令**
8.  **异常处理指令**
9.  **同步控制指令**

在hotspot中每个方法对应的一组**字节码指令**

这组**字节码指令在该方法所对应的栈帧中的局部变量表和操作数栈上进行操作**

**字节码指令包含字节码操作指令 和 操作数** （操作数可能是在局部变量表上也可能在常量池中还可能就是常数）

### 加载与存储指令

> 加载

**加载指令就是把操作数加载到操作数栈中**(可以从局部变量表，常量池中加载到操作数栈)

- 局部变量表加载指令

  -   `i/l/f/d/aload` 后面跟的操作数就是要去局部变量表的哪个槽取值
  -   `iload_0`: 去局部变量表0号槽取出int类型值

- 常量加载指令

  - 可以根据加载的常量范围分为三种（从小到大） `const < push < ldc`

      

> 存储

**存储指令就是将操作数栈顶元素出栈后，存储到局部变量表的某个槽中**

-   存储指令

    -   `i/l/f/d/astore` 后面跟的操作数就是要存到局部变量表的哪个槽
    -   `istore_1`：出栈栈顶int类型的元素保存到局部变量表的1号槽

**注意: 编译时就知道了局部变量表应该有多少槽的位置 和 操作数栈的最大深度（为节省空间，局部变量槽还会复用）**
![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869065097292803.png)


从常量池加载100存储到局部变量表1号槽，从常量池加载200存储到局部变量表2号槽（其中局部变量表0号槽存储this）


### 算术指令

**算术指令将操作数栈中的俩个栈顶元素出栈作运算再将运算结果入栈**

使用的是后缀表达式(逆波兰表达式)，比如 3 4 + => 3 + 4

> 注意

1.  当除数是0时会抛出ArithmeticException异常
2.  浮点数转整数向0取整
3.  浮点数计算精度丢失
4.  Infinity 计算结果无穷大
5.  Nan 计算结果不确定计算值

```java
     public void test1() {
         double d1 = 10 / 0.0;
         //Infinity
         System.out.println(d1);
 
         double d2 = 0.0 / 0.0;
         //NaN
         System.out.println(d2);
 
         //向0取整模式:浮点数转整数
         //5
         System.out.println((int) 5.9);
         //-5
         System.out.println((int) -5.9);
 
 
         //向最接近数舍入模式：浮点数运算
         //0.060000000000000005
         System.out.println(0.05+0.01);
 
         //抛出ArithmeticException: / by zero异常
         System.out.println(1/0);
     }
```



### 类型转换指令

类型转换指令可以分为**宽化类型转换**和**窄化类型转换**(对应基本类型的非强制转换和强制转换)

> 宽化类型转换

**小范围向大范围转换**

-   *int -> long -> float -> double*

    -   `i2l`，`i2f`，`i2d`
    -   `l2f`，`l2d`
    -   `f2d`

byte、short、char 使用int类型的指令

**注意: long转换为float或double时可能发生精度丢失**

```java
     public void test2(){
         long l1 =  123412345L;
         long l2 =  1234567891234567899L;
 
         float f1 = l1;
         //结果: 1.23412344E8 => 123412344
         //                l1 =  123412345L
         System.out.println(f1);
 
         double d1 = l2;
         //结果: 1.23456789123456794E18 => 1234567891234567940
         //                          l2 =  1234567891234567899L
         System.out.println(d1);
     }
```

> 窄化类型转换

**大范围向小范围转换**

-   int->byte、char、short: `i2b`，`i2c`，`i2s`
-   long->int: `l2i`
-   float->long、int: `f2l`，`f2i`
-   double->float、long、int: `d2f`，`d2l`，`d2i`

如果long，float，double要转换为byte，char，short可以先转为int再转为相对应类型

**窄化类型转换会发生精度丢失**

NaN和Infinity的特殊情况:

```java
     public void test3(){
         double d1 = Double.NaN;
         double d2 = Double.POSITIVE_INFINITY;
 
         int i1 = (int) d1;
         int i2 = (int) d2;
         //0
         System.out.println(i1);
         //true
         System.out.println(i2==Integer.MAX_VALUE);
 
         long l1 = (long) d1;
         long l2 = (long) d2;
         //0
         System.out.println(l1);
         //true
         System.out.println(l2==Long.MAX_VALUE);
 
         float f1 = (float) d1;
         float f2 = (float) d2;
         //NaN
         System.out.println(f1);
         //Infinity
         System.out.println(f2);
     }
```

**NaN转为整型会变成0**

**正无穷或负无穷转为整型会变成那个类型的最大值或最小值**






### 对象创建与访问指令

对象创建与访问指令: **创建指令、字段访问指令、数组操作指令、类型检查指令**

#### 创建指令

`new`: 创建实例

`newarray`: 创建一维基本类型数组

`anewarray`: 创建一维引用类型数组

`multianewarray`: 创建多维数组

**注意: 这里的创建可以理解为分配内存,当多维数组只分配了一维数组时使用的是`anewarray`**

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869098378263167.png)


#### 字段访问指令

`getstatic`: 对静态字段进行读操作

`putstatic`: 对静态字段进行写操作

`getfield`: 对实例字段进行读操作

`putfield`: 对实例字段进行写操作

**读操作: 把要进行读操作的字段入栈**

**写操作: 把要写操作的值出栈再写到对应的字段**

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869114227187987.png)


#### 数组操作指令

-   `b/c/s/i/l/f/d/a aload` : **表示将数组中某索引元素入栈** (读)

    -   需要的参数从栈顶依次向下: **索引位置、数组引用**

-   `b/c/s/i/l/f/d/a astore`: **表示将某值出栈并写入数组某索引元素** (写)

    -   需要的参数从栈顶依次向下: **要写入的值、索引位置、数组引用**

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869155713640547.png)


注意: b开头的指令对byte和boolean通用

-   `arraylength`: **先将数组引用出栈再将获得的数组长度入栈**

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869168708228824.png)



#### 类型检查指令

**`instanceof`: 判断某对象是否为某类的实例**

**`checkcast`: 检查引用类型是否可以强制转换**


![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869187011192247.png)


### 总结

由于字节码指令种类多篇幅长，将会分为上、下篇来深入浅出解析字节码指令，本篇作为上篇深入浅出的解析字节码指令介绍、加载存储指令、算术指令、类型转换指令以及对象创建与访问指令

**字节码指令大部分以i、l、f、d、a开头，分别含义对应int、long、float、double、引用，其中byte、char、short、boolean会转换为int来执行**

**字节码指令分为字节码操作指令和需要操作的数据，数据可能来源于局部变量表或常量池**

**加载指令从局部变量表或者常量池中加载数据，存储指令将存储到对应局部变量表的槽中，实例方法的局部变量表的0号槽常用来存储this，如果方法中变量是局部存在的还可能会复用槽**

**算术指令为各种类型和各种算术提供算术规则，在操作数栈中使用后缀表达式对操作数进行算术**

**类型转换分为宽化与窄化，都可能存在精度损失**

**对象创建与访问指令中包含创建对象，访问实例、静态字段，操作数组，类型检查等指令**



上篇文章[深入浅出JVM（九）之字节码指令（上篇）](https://juejin.cn/post/7179239736639225914)已经深入浅出说明加载存储、算术、类型转换的字节码指令，本篇文章作为字节码的指令的下篇，深入浅出的解析各种类型字节码指令，如：方法调用与返回、控制转义、异常处理、同步等


使用idea中的插件jclasslib查看编译后的字节码指令

### 方法调用与返回指令

#### 方法调用指令

**非虚方法: 静态方法，私有方法，父类中的方法，被final修饰的方法，实例构造器**

与之对应不是非虚方法的就是虚方法了

- 普通调用指令

  -   **`invokestatic`: 调用静态方法**
  -   **`invokespecial`: 调用私有方法，父类中的方法,实例构造器<init>方法，final方法**
  -   **`invokeinterface`: 调用接口方法**
  -   **`invokevirtual`: 调用虚方法**

  **使用`invokestatic`和`invokespecial`指令的一定是非虚方法**

  使用`invokeinterface`指令一定是虚方法(因为接口方法需要具体的实现类去实现)

  使用`invokevirtual`指令可能是虚方法

- 动态调用指令

  -   **`invokedynamic`: 动态解析出需要调用的方法再执行**

  jdk 7 出现`invokedynamic`，支持动态语言

> 测试虚方法代码

-   父类

```java
 public class Father {
     public static void staticMethod(){
         System.out.println("father static method");
     }
 
     public final void finalMethod(){
         System.out.println("father final method");
     }
 
     public Father() {
         System.out.println("father init method");
     }
 
     public void overrideMethod(){
         System.out.println("father override method");
     }
 }
```

-   接口

```java
 public interface TestInterfaceMethod {
     void testInterfaceMethod();
 }
```

-   子类

```java
 public class Son extends Father{
 
     public Son() {
         //invokespecial 调用父类init 非虚方法
         super();
         //invokestatic 调用父类静态方法 非虚方法
         staticMethod();
         //invokespecial 调用子类私有方法 特殊的非虚方法
         privateMethod();
         //invokevirtual 调用子类的重写方法 虚方法
         overrideMethod();
         //invokespecial 调用父类方法 非虚方法
         super.overrideMethod();
         //invokespecial 调用父类final方法 非虚方法
         super.finalMethod();
         //invokedynamic 动态生成接口的实现类 动态调用
         TestInterfaceMethod test = ()->{
             System.out.println("testInterfaceMethod");
         };
         //invokeinterface 调用接口方法 虚方法
         test.testInterfaceMethod();
     }
 
     @Override
     public void overrideMethod(){
         System.out.println("son override method");
     }
 
     private void privateMethod(){
         System.out.println("son private method");
     }
 
     public static void main(String[] args) {
         new Son();
     }
 }
```

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869303152248495.png)


#### 方法返回指令

**方法返回指令: 方法结束前，将栈顶元素(最后一个元素)出栈 ，返回给调用者**

根据方法的返回类型划分多种指令

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869323121531000.png)


### 操作数栈管理指令

**通用型指令，不区分类型**

-   出栈

    -   `pop/pop2`出栈1个/2个栈顶元素

-   入栈

    -   `dup/dup2` 复制栈顶1个/2个slot并重新入栈

    -   `dup_x1` 复制栈顶1个slot并插入到栈顶开始的第2个slot下

    -   `dup_x2`复制栈顶1个slot并插入到栈顶开始的第3个slot下

    -   `dup2_x1`复制栈顶2个slot并插入到栈顶开始的第3个slot下

    -   `dup2_x2`复制栈顶2个slot并插入到栈顶开始的第4个slot下

        -   插入到具体的slot计算: dup的系数 + `_x`的系数

### 控制转义指令

#### 条件跳转指令

**通常先进行比较指令，再进行条件跳转指令**

比较指令比较结果-1，0，1再进行判断是否要跳转

**条件跳转指令: 出栈栈顶元素，判断它是否满足条件，若满足条件则跳转到指定位置**

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869338062756524.png)


![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869347881938319.png)



注意: 这种跳转指令一般都"取反"，比如代码中第一个条件语句是d>100，它第一个条件跳转指令就是`ifle`小于等于0，满足则跳转，不满足则按照顺序往下走

#### 比较条件跳转指令

**比较条件跳转指令 类似 比较指令和条件跳转指令 的结合体**

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869369101557850.png)

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869384169627366.png)



#### 多条件分支跳转指令

**多条件分支跳转指令是为了switch-case提出的**

**`tableswitch`用于case值连续的switch多条件分支跳转指令,效率好**

**`lookupswitch`用于case值不连续的switch多条件分支跳转指令(虽然case值不连续,但最后会对case值进行排序)**

> tableswitch

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869395639842388.png)


> lookupswitch

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869413614446793.png)


对于String类型是先找到对应的哈希值再equals比较确定走哪个case的

#### 无条件跳转指令

**无条件跳转指令就是跳转到某个字节码指令处**

`goto`经常使用

`jsr,jsr_w,ret`不怎么使用了

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869432603459860.png)



### 异常处理指令

throw抛出异常对应`athrow`: **清除该操作数栈上所有内容，将异常实例压入调用者操作数栈上**

使用try-catch/try-final/throws时会产生异常表

**异常表保存了异常处理信息** (起始、结束位置、字节码指令偏移地址、异常类在常量池中的索引等信息)

> athrow

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869452045336240.png)



> 异常表

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869464042896658.png)



异常还会被压入栈或者保存到异常表中

### 同步控制指令

synchronized作用于方法时，方法的访问标识会有**ACC_SYNCHRONIZED表示该方法需要加锁**

**synchronized作用于某个对象时，对应着`monitorentry加锁字节码指令和 `monitorexit`解锁字节码指令** 

**Java中的synchronized默认是可重入锁**

-   当线程要访问需要加锁的对象时 (执行monitorentry)

1.  先查看对象头中加锁次数，如果为0说明未加锁，获取后，加锁次数自增
2.  如果不为0，再查看获取锁的线程是不是自己，如果是自己就可以访问，加锁次数自增
3.  如果不为0且获取锁线程不是自己，就阻塞

当线程释放锁时 (执行monitorexit)会让加锁次数自减

![](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728869581168471280.png)


为什么会有2个monitorexit ?

程序正常执行应该是一个monitorentry对应一个monitorexit的

如果程序在加锁的代码中抛出了异常，没有释放锁，那不就会造成其他阻塞的线程永远也拿不到锁了吗

所以在程序抛出异常时(跳转PC偏移量为15的指令)继续往下执行，**抛出异常前要释放锁**

### 总结

本篇文章作为字节码指令的下篇，深入浅出的解析方法调用与返回，操作数栈的入栈、出栈，控制转义，异常和同步相关字节码指令

**方法调用指令分为静态、私有、接口、虚、动态方法等，返回指令则主要是以i、l、f、d、a开头的return指令分别处理不同类型的返回值**

**操作数栈中的出栈指令常用`pop`相关指令，入栈（复制栈顶元素并插入）常用`dup`相关指令**

**控制转义指令中条件跳转指令是判断栈顶元素来进行跳转，比较条件跳转指令是通过两个栈顶元素比较来判断跳转，多条件分支跳转是满足switch，常在异常时进行`goto`无条件跳转**

**异常处理指令用于抛出异常，清除操作数栈并将异常压入调用者操作数栈顶**

**同步控制指令常使用`monitorentry`和`monitoryexit`，为了防止异常时死锁，抛异常前执行`monitoryexit`**

### 最后

-   参考资料

    -   《深入理解Java虚拟机》


本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟~

觉得菜菜写的不错，可以点赞、关注支持哟~

有什么问题可以在评论区交流喔~





## 深入浅出JVM（十一）之如何判断对象“已死”


在方法中会创建大量的对象，对象并不一定是全局都会使用的，并且Java虚拟机的资源是有限的

当JVM（Java虚拟机）判断对象不再使用时，就会将其回收，避免占用资源

那么JVM是如何判断对象不再使用的呢？

本篇文章将围绕判断对象是否再使用，深入浅出的解析引用计数法、可达性分析算法以及JVM如何判断对象是真正的“死亡”（不再使用）

### 判断对象已死

#### 引用计数算法

> 引用计数算法判断对象已死

**在对象添加一个引用计数器，有地方引用此对象该引用计数器+1，引用失效时该引用计数器-1；当引用计数器为0时，说明没有任何地方引用对象，对象可以被回收**

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728898429825691027.png)


但是该方法无法解决*循环引用*（比如对象A的字段引用了对象B，对象B的字段引用了字段A，此时都将null赋值给对象A，B它们的引用计数器上都不为0，也就是表示对象还在被引用，但实际上已经没有引用了）

-   优点 : **标记“垃圾”对象简单，高效**
-   缺点: **无法解决循环引用，存储引用计数器的空间开销，更新引用记数的时间开销**

因为**无法解决循环引用所以JVM不使用引用计数法**

引用计数方法常用在不存在循环引用的时候，比如Redis中使用引用计数，不存在循环引用

> 证明Java未采用引用计数算法

```java
 public class ReferenceCountTest {
     //占用内存
     private static final byte[] MEMORY = new byte[1024 * 1024 * 2];
 
     private ReferenceCountTest reference;
 
     public static void main(String[] args) {
         ReferenceCountTest a = new ReferenceCountTest();
         ReferenceCountTest b = new ReferenceCountTest();
         //循环引用
         a.reference = b;
         b.reference = a;
 
         a = null;
         b = null;
 //        System.gc();
     }
 }
```

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728898464519684310.png)


#### 可达性分析算法

**Java使用可达性分析算法，可以解决循环引用**

> 可达性分析算法判断对象已死

-   从`GC Roots`对象开始，根据引用关系向下搜索，搜索的过程叫做*引用链*

    -   如果通过`GC Roots`可以通过引用链达到某个对象则该对象称为*引用可达对象*
    -   如果通过`GC Roots`到某个对象没有任何引用链可以达到，就把此对象称为*引用不可达对象*，将它放入*引用不可达对象集合*中（如果它是首个引用不可达对象节点，那它就是引用不可达对象根节点）

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728898494530878449.png)


> 可以作为GC Roots对象的对象

1.  在栈帧中局部变量表中引用的对象**参数、临时变量、局部变量**
2.  本地方法引用的对象
3.  方法区的类变量引用的对象
4.  方法区的常量引用的对象(字符串常量池中的引用)
5.  被`sychronized`同步锁持有的对象
6.  JVM内部引用（基础数据类型对应的Class对象、系统类加载器、常驻异常对象等）
7.  跨代引用

-   缺点:

    -   **使用可达性分析算法必须在保持一致性的快照中进行（某时刻静止状态）**
    -   **这样在进行GC时会导致STW(Stop the Word)从而让用户线程短暂停顿**

#### 真正的死亡

真正的死亡最少要经过2次标记

-   通过GC Roots经过可达性分析算法，得到某对象不可达时，进行第一次标记该对象

-   接着进行一次筛选（筛选条件: 此对象是否有必要执行`finalize()`）

    -   如果此对象没有重写`finalize()`或JVM已经执行过此对象的`finalize()`都将被认为此对象没有必要执行`finalize()`，这个对象真正的死亡了

    -   如果认为此对象有必要执行`finalize()`则会把该对象放入`F-Queue`队列中，JVM自动生成一条低优先级的Finalizer线程

        -   *Finalizer线程是守护线程，不需要等到该线程执行完才结束程序，也就是说不一定会执行该对象的finalize()方法*
        -   *设计成守护线程也是为了防止执行finalize()时会发生阻塞，导致程序时间很长，等待很久*
        -   Finalize线程会扫描`F-Queue`队列，如果此对象的`finalize()`方法中让此对象重新与引用链上任一对象搭上关系，那该对象就完成自救***finalize()方法是对象自救的最后机会***

> 测试不重写finalize()方法,对象是否会自救

```java
 /**
  * @author Tc.l
  * @Date 2020/11/20
  * @Description:
  * 测试不重写finalize方法是否会自救
  */
 public class DeadTest01 {
     public  static DeadTest01 VALUE = null;
     public static void isAlive(){
         if(VALUE!=null){
             System.out.println("Alive in now!");
         }else{
             System.out.println("Dead in now!");
         }
     }
     public static void main(String[] args) {
         VALUE = new DeadTest01();
 
         VALUE=null;
         System.gc();
         try {
             //等Finalizer线程执行
             TimeUnit.SECONDS.sleep(1);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
         isAlive();
     }
 }
 /*
 Dead in now!
 */
```

**对象并没有发生自救,对象不再使用“已死”**

> 测试重写finalize()方法,对象是否会自救

```java
 /**
  * @author Tc.l
  * @Date 2020/11/20
  * @Description:
  * 测试重写finalize方法是否会自救
  */
 public class DeadTest02 {
     public  static DeadTest02 VALUE = null;
     public static void isAlive(){
         if(VALUE!=null){
             System.out.println("Alive in now!");
         }else{
             System.out.println("Dead in now!");
         }
     }
 
     @Override
     protected void finalize() throws Throwable {
         super.finalize();
         System.out.println("搭上引用链的任一对象进行自救");
         VALUE=this;
     }
 
     public static void main(String[] args) {
         VALUE = new DeadTest02();
         System.out.println("开始第一次自救");
         VALUE=null;
         System.gc();
         try {
             //等Finalizer线程执行
             TimeUnit.SECONDS.sleep(1);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
         isAlive();
 
         System.out.println("开始第二次自救");
         VALUE=null;
         System.gc();
         try {
             //等Finalizer线程执行
             TimeUnit.SECONDS.sleep(1);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
         isAlive();
     }
 }
 /*
 开始第一次自救
 搭上引用链的任一对象进行自救
 Alive in now!
 开始第二次自救
 Dead in now!
 */
```

**第一次自救成功，第二次自救失败，说明了finalize()执行过，JVM会认为它是没必要执行的了**

**重写finalize()代价高，不能确定各个对象执行顺序，不推荐使用**

### 总结

本篇文章围绕如何判断对象不再使用，深入浅出的解析引用计数法、可达性分析算法以及JVM中如何真正确定对象不再使用的

**引用计数法使用计数器来记录对象被引用的次数，当发生循环引用时无法判断对象是否不再使用，因此JVM没有使用引用计数法**

**可达性分析算法使用从根节点开始遍历根节点的引用链，如果某个对象在引用链上说明这个对象被引用是可达的，不可达对象则额外记录**

**可达性分析算法需要在保持一致性的快照中进行，在GC时会发生STW短暂的停顿用户线程**

**可达性分析算法中的根节点一般是局部变量表中引用的对象、方法中引用的对象、方法区静态变量引用的对象、方法区常量引用的对象、锁对象、JVM内部引用对象等等**

**当对象不可达时，会被放在队列中由finalize守护线程来依次执行队列中对象的finalize方法，如果第一次在finalize方法中搭上引用链则又会变成可达对象，注意finalize方法只会被执行一次，后续再不可达则会被直接认为对象不再使用**


### 最后

- 参考资料

  -   《深入理解Java虚拟机》

  

本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟~

觉得菜菜写的不错，可以点赞、关注支持哟~

有什么问题可以在评论区交流喔~





## 深入浅出JVM（十二）之垃圾回收算法

上篇文章[深入浅出JVM（十一）之如何判断对象“已死”](https://juejin.cn/post/7179783492828397625)已经深入浅出的解析JVM是如何评判对象不再使用，不再使用的对象将变成“垃圾”，等待回收

垃圾回收算法有多种，适用于不同的场景，不同的垃圾收集器使用不同的算法

本篇文章将围绕垃圾回收算法，深入浅出的解析垃圾回收分类以及各种垃圾回收算法

### 垃圾回收算法

#### 垃圾回收分类

垃圾收集器有着多种GC方式，不同的GC方式有自己的特点，回收的堆内存部分也不同

堆内存分为新生代和老年代，新生代存储“年轻”的对象，老年代存储“老”或内存大的对象，对象年龄由经历多少次GC来判断

其中整堆收集时不仅会回收整个堆还会回收元空间（直接内存）

*   **部分收集(Partial GC): 收集目标是部分空间**

    *   **新生代收集(Minor GC/ Young GC)：收集新生代Eden、Survive to、Survive from区**
    *   **老年代收集(MajorGC/Old GC)：收集老年代**
    *   **混合收集(Mixed GC)：收集整个新生代和部分老年代**

*   **整堆收集(Full GC): 整个堆 + 元空间**

#### 标记-清除算法

**Mark-Sweep**

**标记：从GCRoots开始遍历引用链，标记所有可达对象 (在对象头中标记)**

**清除：从堆内存开始线性遍历，发现某个对象没被标记时对它进行回收**
![](https://bbs-img.huaweicloud.com/blogs/img/20241015/1728986104658344069.png)



标记-清除算法实现简单、不需要改变引用地址，但是需要两次遍历扫描效率不高，并且会出现内存碎片

注意：这里的清除并不是真正意义上的回收内存，只是更新空闲列表（标记这块内存地址为空闲，后续有新对象需要使用就覆盖）

#### 复制算法

**Copying**

Survive区分为两块容量一样的Survive to区和Survive from区

每次GC将Eden区和Survive from区存活的对象放入Survive to区，此时Survive to区改名为Survive from区，原来的Survive from区改名为Survive to区 ***保证Survive to区总是空闲的***

如果Survive from区的对象经过一定次数的GC后(默认15次)，把它放入老年代

> 流程图

注意：图中的dean为Eden区（写错）
![](https://bbs-img.huaweicloud.com/blogs/img/20241015/1728986119450354876.png)

![](https://bbs-img.huaweicloud.com/blogs/img/20241015/1728986139093299186.png)



注意：最终的survive from区存活对象占用的内存应该是（那两块蓝色）挨着一起的，图中为了标识字分开来了

**复制算法不需要遍历，并且不会产生内存碎片，但是会浪费survive区一半的内存，移动对象时需要STW暂停用户线程，并且复制后会改变引用地址**（hotspot使用直接指针访问，还要改变栈中reference执行新引用地址）

如果复制算法中对象存活率太高会导致十分消耗资源，因此一般只有新生代才使用复制算法

#### 标记-整理算法

**Mark-Compact**

**标记：从GCRoots开始遍历引用链，标记所有可达对象（在对象头中标记）** （与标记-清除算法一致）

**整理：让所有存活对象往内存空间一端移动，然后直接清理掉边界以外的所有内存**
![](https://bbs-img.huaweicloud.com/blogs/img/20241015/1728986156858598262.png)



**标记-整理算法不会出现内存碎片也不会浪费空间，但是效率低（比标记-清除还多了整理功能），移动对象导致STW和改变reference指向**

如果不移动对象会产生内存碎片，内存碎片过多，将无法为大对象分配内存

还有种方法:多次标记-清除，等内存碎片多了再进行标记-整理

#### 分代收集算法

|                      | 标记清除mark-sweep | 复制copying    | 标记整理mark-compact |
| -------------------- | ------------------ | -------------- | -------------------- |
| 速度                 | 中                 | 快             | 慢                   |
| GC后是否需要移动对象 | 不移动对象         | 移动对象       | 移动对象             |
| GC后是否存在内存碎片 | 存在内存碎片       | 不存在内存碎片 | 不存在内存碎片       |

需要移动对象 意味着 要改变改对象引用地址 也就是说要改变栈中`reference`指向改对象的引用地址，并且会发生STW停顿用户线程

当空间中存在大量内存碎片时，可能导致大对象无法存储

**分代收集算法 : 对待不同生命周期的对象可以采用不同的回收算法**（不同场景采用不同算法）

**年轻代: 对象生命周期短、存活率低、回收频繁，采用复制算法，高效**

**老年代: 对象生命周期长、存活率高、回收没年轻代频繁，采用标记-清除 或混用 标记-整理**

#### 增量收集算法

mark-sweep、copying、mark-compact算法都存在STW，如果垃圾回收时间很长，会严重影响用户线程的响应

**增量收集算法: 采用用户线程与垃圾收集线程交替执行**

**增量收集算法能够提高用户线程的响应时间，但存在GC、用户线程切换的开销，降低了吞吐量，GC成本变大**

#### 分区算法

堆空间越大，GC时间就会越长，用户线程响应就越慢

**分区算法: 将堆空间划分为连续不同的区，根据要求的停顿时间合理回收n个区，而不是一下回收整个堆**

每个区独立使用，独立回收，根据能承受的停顿时间控制一次回收多少个区

G1收集器以及两块低延迟收集器Shenandoah、ZGC就使用到这种分区算法

### 总结

本篇文章围绕垃圾回收算法，深入浅出解析垃圾回收分类、标记清除、复制、标记整理、分代收集、增量收集、分区算法等多种算法

**从垃圾回收空间上划分可以分为Full GC回收整个堆加上元空间、Minor GC回收新生代、major GC回收老年代、mixed GC回收新生代加老年代**

**标记清除算法会遍历引用链标记可达对象从而清理不可达对象，会产生内存碎片，速度一般**

**复制算法不会产生内存碎片，并且速度很快，但是会浪费survivor区一半空间，并且会移动对象**

**标记整理算法在标记清理基础上增加整理功能，不会产生内存碎片，但会移动对象，速度慢**

**不同的算法有不同的特点，应对新生代可以使用复制算法，应对老年代可以使用标签清除/整理算法**

**增量收集使用GC、用户线程交替执行，虽然降低用户响应，但线程切换、吞吐量下降会增加GC成本**

**分区算法将堆内存划分为多个区，根据能够接收的停顿时间来回收性价比高的多个区，停顿时间既在能够接收时间内，又能够回收性能比高的区**

### 最后（一键三连求求拉\~）

本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟\~

本篇文章笔记以及案例被收入 [gitee-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava")、 [github-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava") 感兴趣的同学可以stat下持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多干货，公众号：菜菜的后端私房菜







## 深入浅出JVM（十三）之垃圾回收算法细节


上篇文章[深入浅出JVM（十二）之垃圾回收算法](https://juejin.cn/post/7180141777893425208)讨论了垃圾回收算法，为了能够更加充分的理解后续的垃圾收集器，本篇文章将深入浅出解析垃圾回收算法的相关细节，如：STW、枚举根节点如何避免长时间STW、安全点与安全区、跨代引用引起的GC Root扫描范围增大等问题

### HotSpot垃圾回收算法细节

#### STW

**Stop The Word**

**STW: GC中为了分析垃圾过程确保一致性，会导致所有Java用户线程停顿**

**可达性分析算法枚举根节点导致STW**

因为不停顿线程的话，在分析垃圾的过程中引用会变化，这样分析的结果会不准确

#### 根节点枚举

枚举GC Roots的过程是耗时的，因为要找到栈上的Reference作为根节点，这就不得不对整个栈进行扫描

为了避免枚举根节点耗费太多时间，使用OopMap（Ordinary Object Pointer普通对象指针）数据结构来记录reference引用位置

**根节点枚举必须暂停用户线程，因为要保证一致性的快照（根节点枚举是用户线程停顿的重要原因）**

如果单纯遍历GC Roots和引用链过程会非常的耗时，**使用OopMap记录引用所在位置，扫描时不用去方法区全部扫描**

**使用OopMap快速，精准的让HotSpot完成根节点枚举**

#### 安全点与安全区域

> safe point

代码中引用的位置可能发生变动，每时每刻更新OopMap的开销是非常大的，因此规定在安全点位置才更新OopMap

那什么位置才是安全点呢？

**安全点所在的位置一般具有让程序长时间执行的特征**，比如方法调用、循环、异常跳转等

由于只有安全点位置的OopMap是有效的，因此在**进行GC时用户线程需要停留在安全点**

让用户线程到最近的安全点停下来的方式有两种，分别是抢先式中断、主动式中断

抢先式中断: 垃圾收集发生时，中断所有用户线程，如果有用户线程没在安全点上就恢复它再让它执行会到安全点上

**主动式中断: 设置一个标志位，当要发生垃圾回收时，就把这个标记位设置为真，用户线程执行时会主动轮询查看这个标志位，一旦发现它为真就去最近的安全点中断挂起**

hotspot选择主动式中断，使用内存保护陷阱方式将轮循标志位实现的只有一条汇编指令，高效

**安全点设立太多会影响性能，设立太少可能会导致GC等待时间太长**

**安全点保证程序线程执行时，在不长时间内就能够进入垃圾收集过程的安全点**

> safe region

安全点只能保证程序线程执行时，在不长时间内进入安全点，如果是Sleep或者Blocking的线程呢?

**安全区域：确保某一段代码中，引用关系不发生变化，这段区域中任意地方开始垃圾收集都是安全的**

sleep、blocking线程需要停留在安全区才能进行GC

用户线程执行到安全区，会标识自己进入安全区，垃圾回收时就不会去管这些标识进入安全区的线程

用户线程要离开安全区时，会去检查是否执行完根节点枚举，执行完了就可以离开，没执行完就等待，直到收到可以离开的信号（枚举完GC Roots）

#### 记忆集与卡表

前面说到过分代收集的概念，比如GC可能是只针对年轻代的，但年轻代对象可能引用老年代，对了可达性分析的正确性可能要将老年代也加入GC Roots的扫描范围中，这无疑又增加了一笔开销

上述问题叫做跨代引用问题，跨代引用问题不仅仅只存在与年轻代与老年大中，熟悉G1、低延迟ZGC、Shenandoah收集器的同学会知道它们分区region也会存在这种跨代引用

使用记忆集来记录存在跨代引用的情况，当发生跨代引用时只需要将一部分跨代引用的加入GC Roots的扫描范围，而不用全部扫描

可以把记忆集看成**记录从非收集区指向收集区的指针集合**

常用卡表实现记忆集的卡精度（每个记录精确到内存区，该区域有对象有跨代指针）

卡表简单形式是一个字节数组，数组中每个元素对应着其标识内存区域中一块特定大小的内存区（这块内存区叫:卡页）

![](https://bbs-img.huaweicloud.com/blogs/img/20241022/1729560118299996670.png)



如果卡页上有对象含有跨代指针，就把对应卡表数组值改为1(卡表变脏)，1说明卡表对应的内存块有跨代指针，把卡表数组上元素为1的内存块加入GC Roots中一起扫描（图中卡表绿色位置表示卡表变脏存在跨代引用）

**记忆集解决跨代引用问题，缩减GC Roots扫描范围**

#### 写屏障

维护卡表变脏应该放在跨代引用赋值之后，**使用写屏障来在跨代引用赋值操作后进行更新卡表**

这里的写屏障可以理解为AOP，在赋值完成后进行更新卡表的状态

更新卡表操作产生额外的开销，在高并发情况下还可能发生伪共享问题，降低性能

可以不采用无条件的写屏障，先检查卡表标记，只有未被标记过时才将其标记为变脏，来避免伪共享问题，但会增加额外判断的开销

-XX:+UseCondCardMark 是否开启卡表更新条件判断，开启增加额外判断的开销，可以避免伪共享问题

### 总结

本篇文章围绕垃圾回收算法细节深入浅出解析STW、根节点枚举避免长时间STW、安全区与安全区域、记忆集解决跨代引用增大GC Root扫描范围、维护卡表的写屏障等

**为了避免用户线程改变引用关系，能够正确的进行可达性分析，需要stop the word 停止用户线程**

**枚举GC Roots时为了避免长时间的STW，使用OopMap记录引用位置，避免扫描方法区**

**由于引用关系的变化，实时更新维护OopMap的开销是很大的，只有在循环、异常跳转、方法调用位置的安全点才更新OopMap，因此只有在安全点中才能正确的进行GC**

**安全区可以看成扩展的安全点，在一块代码中不会改变引用关系；对于sleep、blocking状态的用户线程来说，只需要在安全区就能够进行GC**

**hotspot采用主动轮循式中断，用户线程运行时主动轮循判断是否需要进行GC，需要进行GC则到附近最近的安全点/区，GC时不会管理这些进入安全区的用户线程，当用户线程要离开安全区时检查是否枚举完GC Root，枚举完则可以离开否则等待**

**跨代引用可能增加GC Root扫描范围，使用卡表实现记忆集管理跨代引用，当卡表中的卡页变脏时说明那块内存存在跨代引用，需要加入扫描范围；记忆集有效减少了扫描范围**

**使用类似AOP的写屏障维护卡表状态，高并发情况下可能出现伪共享问题，可以开启增加额外条件判断再进行维护卡表状态，增加条件判断开销但可以避免伪共享问题**

### 最后（一键三连求求拉~）

本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟~

本篇文章笔记以及案例被收入 [gitee-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava")、 [github-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava") 感兴趣的同学可以stat下持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多干货，公众号：菜菜的后端私房菜











## 深入浅出JVM（十四）之内存溢出、泄漏与引用


本篇文章将深入浅出的介绍Java中的内存溢出与内存泄漏并说明强引用、软引用、弱引用、虚引用的特点与使用场景

### 引用



在栈上的`reference`类型存储的数据代表某块内存地址，称`reference`为某内存、某对象的引用

实际上引用分为很多种，从强到弱分为：**强引用 > 软引用 > 弱引用 > 虚引用**

平常我们使用的引用实际上是强引用，各种引用有自己的特点，下文将一一介绍

强引用就是Java中普通的对象，而软引用、弱引用、虚引用在JDK中定义的类分别是`SoftReference`、`WeakReference`、`PhantomReference`

下图是软引用、弱引用、虚引用、引用队列（搭配虚引用使用）之间的继承关系
![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241023/1729646300289485878.png)



#### 内存溢出与内存泄漏

为了更清除的描述引用之间的作用，首先需要介绍一下内存溢出和内存泄漏

当发生内存溢出时，表示**JVM没有空闲内存为新对象分配空间，抛出`OutOfMemoryError(OOM)`**

当应用程序占用内存速度大于垃圾回收内存速度时就可能发生OOM

抛出OOM之前通常会进行Full GC，如果进行Full GC后依旧内存不足才抛出OOM

JVM参数`-Xms10m -Xmx10m -XX:+PrintGCDetails`

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241023/1729646328528702104.png)


内存溢出可能发生的两种情况：

1.  必须的资源确实很大，**堆内存设置太小** （通过`-Xmx`来调整）

<!---->

2.  **发生内存泄漏**，创建大量对象，且生命周期长，不能被回收

**内存泄漏Memory Leak: 对象不会被程序用到了，但是不能回收它们**

对象不再使用并且不能回收就会一直占用空间，大量对象发生内存泄漏可能发生内存溢出OOM

广义内存泄漏：不正确的操作导致对象生命周期变长

1.  单例中引用外部对象，当这个外部对象不用了，但是因为单例还引用着它导致内存泄漏
2.  一些需要关闭的资源未关闭导致内存泄漏

#### 强引用

强引用是程序代码中普遍存在的引用赋值，比如`List list = new ArrayList();`

**只要强引用在可达性分析算法中可达时，垃圾收集器就不会回收该对象，因此不当的使用强引用是造成Java内存泄漏的主要原因**

#### 软引用

当内存充足时不会回收软引用

**只有当内存不足时，发生Full GC时才将软引用进行回收，如果回收后还没充足内存则抛出OOM异常**

JVM中针对不同的区域（年轻代、老年代、元空间）有不同的GC方式，**Full GC的回收区域为整个堆和元空间**

软引用使用`SoftReference`

> 内存充足情况下的软引用

```java
     public static void main(String[] args) {
         int[] list = new int[10];
         SoftReference listSoftReference = new SoftReference(list);
         //[I@61bbe9ba
         System.out.println(listSoftReference.get());
     }
```

> 内存不充足情况下的软引用(JVM参数:-Xms5m -Xmx5m -XX:+PrintGCDetails)

```java
 //-Xms5m -Xmx5m -XX:+PrintGCDetails
 public class SoftReferenceTest {
     public static void main(String[] args) {
         int[] list = new int[10];
         SoftReference listSoftReference = new SoftReference(list);
         list = null;
 
         //[I@61bbe9ba
         System.out.println(listSoftReference.get());
 
         //模拟空间资源不足
         try{
             byte[] bytes = new byte[1024 * 1024 * 4];
             System.gc();
         }catch (Exception e){
             e.printStackTrace();
         }finally {
             //null
             System.out.println(listSoftReference.get());
         }
     }
 }
```

#### 弱引用

**无论内存是否足够，当发生GC时都会对弱引用进行回收**

弱引用使用`WeakReference`

> 内存充足情况下的弱引用

```java
     public static void test1() {
         WeakReference<int[]> weakReference = new WeakReference<>(new int[1]);
         //[I@511d50c0
         System.out.println(weakReference.get());
 
         System.gc();
         
         //null
         System.out.println(weakReference.get());
     }
```

> WeakHashMap

JDK中有一个WeakHashMap，使用与Map相同，只不过节点为弱引用

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241023/1729646353897525168.png)


当key的引用不存在引用的情况下，发生GC时，WeakHashMap中该键值对就会被删除

```java
     public static void test2() {
         WeakHashMap<String, String> weakHashMap = new WeakHashMap<>();
         HashMap<String, String> hashMap = new HashMap<>();
 
         String s1 = new String("3.jpg");
         String s2 = new String("4.jpg");
 
         hashMap.put(s1, "图片1");
         hashMap.put(s2, "图片2");
         weakHashMap.put(s1, "图片1");
         weakHashMap.put(s2, "图片2");
 
         //只将s1赋值为空时,堆中的3.jpg字符串还会存在强引用,所以要remove
         hashMap.remove(s1);
         s1=null;
         s2=null;
 
         System.gc();
 
         //4.jpg=图片2
         test2Iteration(hashMap);
 
         //4.jpg=图片2
         test2Iteration(weakHashMap);
     }
 
     private static void test2Iteration(Map<String, String>  map){
         Iterator iterator = map.entrySet().iterator();
         while (iterator.hasNext()){
            Map.Entry entry = (Map.Entry) iterator.next();
             System.out.println(entry);
         }
     }
```

未显示删除weakHashMap中的该key，当这个key没有其他地方引用时就删除该键值对

> 软引用，弱引用适用的场景

数据量很大占用内存过多可能造成内存溢出的场景

比如需要加载大量数据，全部加载到内存中可能造成内存溢出，就可以使用软引用、弱引用来充当缓存，当内存不足时，JVM对这些数据进行回收

使用软引用时，可以自定义Map进行存储`Map<String,SoftReference<XXX>> cache`

使用弱引用时，则可以直接使用`WeakHashMap`

软引用与弱引用的区别则是GC回收的时机不同，软引用存活可能更久，Full GC下才回收；而弱引用存活可能更短，发生GC就会回收

#### 虚引用

**使用`PhantomReference`创建虚引用，需要搭配引用队列`ReferenceQueue`使用**

**无法通过虚引用得到该对象实例**（其他引用都可以得到实例）

**虚引用只是为了能在这个对象被收集器回收时收到一个通知**

> 引用队列搭配虚引用使用

```java
 public class PhantomReferenceTest {
     private static PhantomReferenceTest reference;
     private static ReferenceQueue queue;
 
     @Override
     protected void finalize() throws Throwable {
         super.finalize();
         System.out.println("调用finalize方法");
         //搭上引用链
         reference = this;
     }
 
     public static void main(String[] args) {
         reference = new PhantomReferenceTest();
         //引用队列
         queue = new ReferenceQueue<>();
         //虚引用
         PhantomReference<PhantomReferenceTest> phantomReference = new PhantomReference<>(reference, queue);
         
         Thread thread = new Thread(() -> {
             PhantomReference<PhantomReferenceTest> r = null;
             while (true) {
                 if (queue != null) {
                     r = (PhantomReference<PhantomReferenceTest>) queue.poll();
                     //说明被回收了,得到通知
                     if (r != null) {
                         System.out.println("实例被回收");
                     }
                 }
             }
         });
         thread.setDaemon(true);
         thread.start();
 
 
         //null (获取不到虚引用)
         System.out.println(phantomReference.get());
 
         try {
             System.out.println("第一次gc 对象可以复活");
             reference = null;
             //第一次GC 引用不可达 守护线程执行finalize方法 重新变为可达对象
             System.gc();
             TimeUnit.SECONDS.sleep(1);
             if (reference == null) {
                 System.out.println("object is dead");
             } else {
                 System.out.println("object is alive");
             }
             reference = null;
             System.out.println("第二次gc 对象死了");
             //第二次GC 不会执行finalize方法 不能再变为可达对象
             System.gc();
             TimeUnit.SECONDS.sleep(1);
             if (reference == null) {
                 System.out.println("object is dead");
             } else {
                 System.out.println("object is alive");
             }
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
 
     }
 }
```

结果:

```java
 /*
 null
 第一次gc 对象可以复活
 调用finalize方法
 object is alive
 第二次gc 对象死了
 实例被回收
 object is dead
 */
```

第一次GC时，守护线程执行finalize方法让虚引用重新可达，所以没死

第二次GC时，不再执行finalize方法，虚引用已死

虚引用回收后，引用队列有数据，来通知告诉我们reference这个对象被回收了

> 使用场景

GC只能回收堆内内存，而直接内存GC是无法回收的，直接内存代表的对象创建一个虚引用，加入引用队列，当这个直接内存不使用，这个代表直接内存的对象为空时，这个虚内存就死了，然后引用队列会产生通知，就可以通知JVM去回收堆外内存（直接内存）

### 总结

本篇文章围绕引用深入浅出的解析内存溢出与泄漏、强引用、软引用、弱引用、虚引用

**当JVM没有足够的内存为新对象分配空间时就会发生内存溢出抛出OOM**

**内存溢出有两种情况，一种是分配的资源太少，不满足必要对象的内存；另一种是发生内存泄漏，不合理的设置对象的生命周期、不关闭资源都会导致内存泄漏**

**使用最常见的就是强引用，强引用只有在可达性分析算法中不可达时才会回收，强引用使用不当是造成内存泄漏的原因之一**

**使用`SoftReference`软引用时，只要内存不足触发Full GC时就会对软引用进行回收**

**使用`WeakReference`弱引用时，只要发生GC就会对弱引用进行回收**

**软、弱引用可以用来充当大数据情况下的缓存，它们的区别就是软引用可能活的更久Full GC才回收，使用弱引用时可以直接使用JDK中提供的WeakHashMap**

**虚引用无法在程序中获取，与引用队列搭配使用，当虚引用被回收时，能够从引用队列中取出（感知），可以在直接引用不使用时，发出消息让JVM进行回收**

### 最后（一键三连求求拉~）

本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟~

本篇文章笔记以及案例被收入 [gitee-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava")、 [github-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava") 感兴趣的同学可以stat下持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多干货，公众号：菜菜的后端私房菜







## 深入浅出JVM（十五）之垃圾收集器（上篇）

不同场景可以使用不同的垃圾收集器来进行GC，不同的垃圾收集器有不同的特点，有的单线程执行适合单核、有的多线程并发执行、有的追求高吞吐量、有的追求低延迟...

在学习垃圾收集器前需要学习一定的前置知识如JVM运行时数据区、垃圾回收算法等，需要的同学可以查看该专栏下以前的文章，如

[深入浅出JVM（二）之运行时数据区和内存溢出异常](https://juejin.cn/post/7169228223132205092)

[深入浅出JVM（十二）之垃圾回收算法](https://juejin.cn/post/7180141777893425208)

[深入浅出JVM（十三）之垃圾回收算法细节](https://juejin.cn/post/7181249495793926181)

垃圾收集器的内容细节都比较多，文章将会分为上、中、下三篇

在上篇中主要介绍垃圾回收器的分类、性能指标以及串行与并行的垃圾收集器

在中篇中主要介绍并发的垃圾收集器以及并发执行带来的问题以及解决方案

在下篇中主要介绍低延迟的垃圾收集器

### GC分类与性能指标

#### 垃圾回收器分类

根据GC线程数进行分类，可以分为**单GC线程串行和多GC线程并行的垃圾回收器**

单线程串行顾名思义就是单线程执行GC，多线程并行就是多个线程同时执行GC（需要多核）

根据GC工作模式进行分类，可以分为**同一时刻GC线程独占式和用户、GC线程并发的垃圾回收器**

GC线程独占式是GC线程执行时用户线程需要停顿，而用户、GC线程并发式就是用户、GC线程并发执行

根据处理内存碎片进行分类，可以分为**压缩式（无碎片内存）和非压缩式的垃圾回收器**

根据处理内存区间进行分类，可以分为**新生代、老年代的垃圾回收器**

#### 性能指标

在介绍垃圾回收器前，需要了解两个性能指标：**吞吐量和延迟**

在程序运行时，由于GC的存在，在进行GC时需要额外的开销，我们希望垃圾回收器能够有高吞吐量，尽早的完成GC，将运行时间留给用户线程去处理我们的程序

但因为种种原因可能导致GC的时间过长，这样就会延迟用户线程的执行，我们总是希望这种延迟是低的

GC的目标尽量追求**高吞吐量和低延迟**

高吞吐量表示GC处理的快，低延迟在交互程序中能给用户带来好的响应

但是高吞吐量和低延迟是冲突的

**以高吞吐量为优先，就要减少GC频率，这样会导致GC需要更长的时间，从而导致延迟升高**

**以低延迟为优先，为了降低每次GC暂停更短的时间，只能增大GC频率，这样导致吞吐量降低**
![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733029693493140.png)



现在GC的目标是**在高吞吐量优先的情况下尽量降低延迟；在低延迟优先的情况下尽量增大吞吐量**

### 垃圾收集器

以GC线程运行状态来分类，经典的垃圾收集器分为**串行、并行、并发垃圾收集器**，现在还有两款**低延迟垃圾收集器**

串行垃圾收集器: `Serial` 、`Serial Old`

并行垃圾收集器: `ParNew` 、`Parallel Scavenge` , `Parallel Old`

并发垃圾收集器: `G1` 、`CMS`

低延迟垃圾收集器: `ZGC`、 `Shenandoah`

垃圾收集器可能只处理年轻代（新生代）或老年代，也可能对内存区域都进行处理

因为垃圾收集器可能只处理部分空间，因此要做到Full GC时，需要搭配其他垃圾收集器一起进行GC

比如Serial GC与Serial Old GC（图中黑线）

![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733051811140578.png)



图中位于白色部分是处理年轻代的垃圾收集器，位于灰色部分是处理老年代的垃圾收集器，其中G1都会处理

垃圾收集器之间会进行搭配使用，但在高版本中可能移除这种搭配关系，也可能移除垃圾收集器

#### 串行垃圾收集器

串行垃圾收集器主要有两种，分别处理年轻代与老年代，它们互相搭配使用

**Serial收集器使用复制算法处理年轻代**

**Serial Old收集器使用标记-整理算法处理老年代**

> (新生代)Serial收集器 + (老年代)Serial Old 收集器 运行图
> ![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733066572835595.png)



串行收集器(单线程)，简单高效(在单线程中)、内存开销最小

收集过程中，必须暂停其他所有用户线程，直到它收集结束

**默认Client模式的收集器，适合单核CPU** 不适合交互式

> JVM参数设置

**`-XX:+UseSerialGC` :新生代使用Serial GC 老年代使用 Serial Old GC**

![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733088067884568.png)



#### 并行垃圾收集器

##### ParNew收集器

ParNew是Serial的并行版本，**使用复制算法处理新生代**

> (新生代)ParNew收集器 + (老年代)Serial Old收集器 运行图
> ![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733111299554722.png)



在单核情况下，Serial会比ParNew高效；在多核情况下，ParNew吞吐量会更高

年轻代使用ParNew时，老年代只能使用串行的Serial Old

> JVM参数设置

`-XX:+UseParNewGC`新生代使用ParNew

`-XX:ParallelGCThreads=线程数` 并行执行的GC线程数量

![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733128221514459.png)



##### Parallel Scavenge收集器 和 Parallel Old收集器

Parallel Scavenge 是**吞吐量优先的并行收集器**，使用**复制算法处理新生代**

Parallel Old **使用标记-整理算法处理老年代**

> Parallel Scavenge收集器 + Parallel Old收集器运行图



![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733139713202363.png)




与ParNew的不同：**精确控制吞吐量，自适应调节策略**

**吞吐量越高说明最高效率利用处理器资源完成程序**

> 参数设置

-   `-XX:UseParallelGC` 或 `-XX:UseParallelOldGC`互相激活

    -   新生代使用ParallelGC 老年代使用Parallel Old GC

<!---->

-   `-XX:MaxGCPauseMillis`

    -   最大垃圾收集停顿时间

-   `-XX:GCTimeRatio`

    -   控制GC时间 `-XX:GCTimeRatio=99` 时 GC时间占比 = 1 / (1 + 99) = 1%

-   `-XX:+UseAdaptiveSizePolicy`

    -   **自适应调节策略** 默认开启
    -   开启时JVM会根据系统运行情况动态调整`-XX:MaxGCPauseMillis`和`-XX:GCTimeRatio`参数来设置最合适的停顿时间和GC时间

> 吞吐量优先垃圾收集器是JDK8默认使用的垃圾收集器

![](https://bbs-img.huaweicloud.com/blogs/img/20241024/1729733177840581446.png)


### 总结

本篇文章作为垃圾收集器系列文章的上篇，主要介绍从各个方面对垃圾收集器的分类、GC性能指标、串行垃圾收集器、并行垃圾收集器等

**垃圾收集器可以划分为串行、并行、并发垃圾收集器，其中串行表示单GC线程独自执行、并行表示多GC线程同时刻执行、并发表示GC、用户线程并发执行**

**发生GC时需要考虑到的性能指标是高吞吐量（GC执行效率高）、低延迟（GC时的停顿时间尽量低），这两个指标往往不能都满足，不同的垃圾收集器有不同特点适合在不同场景下发挥作用**

**串行垃圾收集器Serial使用复制算法回收年轻代，搭配Serial Old使用标记-整理算法回收老年代，适合在单核、延迟不敏感的场景下使用，Client模式下的默认垃圾收集器**

**并行垃圾收集器ParNew可以看成Serial的并行版本，使用复制算法多GC线程并行回收年轻代，老年代使用Serial Old单GC线程回收老年代**

**吞吐量优先垃圾收集器Parallel Scavenge使用复制算法多GC线程并行回收年轻代，搭配Parallel Old使用标记-整理算法多GC线程并行回收老年代，提供最大停顿时间、GC时间占比、自适应调节等参数来让用户自定义使用，Server模式下默认垃圾收集器**

并发垃圾收集器以及用户、GC线程并发执行带来的问题与解决方案将在垃圾收集器系列的中篇介绍

低延迟垃圾收集器将在垃圾收集器系列的下篇中介绍

### 最后（一键三连求求拉~）

本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟~

本篇文章笔记以及案例被收入 [gitee-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Ftcl192243051%2FStudyJava")、 [github-StudyJava](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava "https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2FTc-liang%2FStudyJava") 感兴趣的同学可以stat下持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多干货，公众号：菜菜的后端私房菜











## 末尾
