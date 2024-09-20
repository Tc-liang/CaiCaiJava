# RocketMQ

## RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件

消息队列MessageQueue，简称MQ

在队列的基础上，加入生产者与消费者模型，使用队列作为载体就能够组成简单的消息队列，在队列中“运输”的数据被称为消息

![](https://bbs-img.huaweicloud.com/blogs/img/20240820/1724143384843501287.png)

消息队列可以在单节点内存中使用，也可以作为分布式存储的中间件来使用

由于项目的架构组织，目前常接触的消息队列往往是作为分布式存储的消息中间件来使用，比如：RabbitMQ、RocketMQ、Kafka等

内存队列相比于消息中间件往往有**轻量、低延迟（无需网络通信）、简单易用的特点**，但也存在**不能持久化（消息丢了怎么办？）、无法扩展（消息量太大怎么办？）的缺陷**

而**消息中间件的特点较多如：持久化、高可用、集群扩展、负载均衡、系统解耦等特点，但同时也会增加调用链路、提升系统复杂度**，因此常用于分布式系统中

> 特点

**异步通信：MQ提供异步通信，无需同步等待，适合需要异步场景**

**持久化：消息会进行持久化，持久化后无需担心异步通信的消息会丢失**

**削峰填谷：面对突发流量，MQ相当于缓冲区，防止后端服务短时间内接收过多请求导致服务崩溃**

**系统解耦：松耦合，生产者（调用方）、消费者（被调用方）可以独立升级/扩展**

**集群：与其他中间件集群类似，方便水平/垂直扩展，提高系统吞吐量/可用性**

消息中间件除了这些特点外，还有它们独有的功能与特点，本文就从RocketMQ开始，快速入门消息中间件专栏

在专栏中一步步解析消息中间件的架构、流程、原理、源码等，再分析各种消息中间件的优势以及适用场景

#### RocketMQ架构概念

*   **Message：消息为MQ中运输的载体，存储需要传输的数据与其他元数据**

*   **MessageQueue：消息队列用于存储消息，内部通过偏移量能够找到消息**

    *   **分为读写队列用于消费时读和持久化消息时写，通常队列数量相同**

    *   **队列ID使用数量0开始并逐步进行自增，比如分配3个读写队列，那么id分别为0、1、2**

*   **Topic：主题（类似Kafka中的分区），生产者需要将消息发送到对应的Topic上，消费者需要订阅对应的Topic进行消费**
    *   **充当消息的分类，过滤消息**，比如不同业务(量级)的消息分发到对应的Topic中（order、pay、cart、user...）
    *   **Topic中存在多个队列（MQ）用于存储消息，增加topic下的队列能够提高消息的水平写入能力**
    *   **Topic可以存在不同的broker上，保证高可用**

*   **Tag：主题下的二级分类，过滤消息**

*   **Broker：存储消息、MessageQueue、Topic等元数据的服务端，用于接收消息（处理生产）、持久化消息、查找消息（处理消费）**
    *   **接收消息只能由主节点接收并持久化，从节点只用于同步对应主节点消息**
    *   **消费消息既可以通过主节点拉取消息，也可以通过从节点**

通过下面两个主节点的Broker图，很容易的可以理解它们的关系：

1.  **Broker为存储消息的服务端，其中包含多个Topic**
2.  **Topic是用于生产、消费订阅的主题，其中为了能够水平扩展写入性能可以设置多个MessageQueue，MessageQueue的ID从0开始进行自增**
3.  **为了保证高可用，不同的Broker也会存在相同的Topic，只是其中的队列不同，防止broker意外宕机时服务不可用**，如图中的TopicA，0、1队列在Broker A中，而2、3队列在Broker B中

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240822/1724294678060157179.png)

*   **NameServer：存储broker路由信息、类似注册中心**
    *   **与broker通信，存储其数据如：Topic、MessageQueue等数据，顺便进行心跳判断broker是否离线**
    *   **与producer、consumer通信，将broker元数据进行传输，这样无论是生产还是消费都能根据数据找到对应的Broker、Topic、MQ等**

*   **Product：生产者，用于生产消息，并把消息发送到消息队列**
    *   **相同配置的生产者成组Group可以协调工作**
    *   **通过NameServer通信获取到的路由信息，根据负载均衡算法选择对应的Topic以及队列ID发送到对应Broker中进行持久化**

*   **Comsumer：消费者，用于消费消息，从消息队列拉取消息(长轮询)进行消费消息**
    *   **队列中使用偏移量确认消息，消费时同理也使用偏移量标识消费到的位置**
    *   **消费模式分为广播、集群模式，广播模式就是发布订阅模型，集群模式为点对点消费**
    *   **拉取消息利用长轮询机制弥补实时性差的特点，但大量长连接会导致开销大（后文详细描述长轮询机制）**
    *   **通过NameServer通信获取到的路由信息，消费者根据消费模式（广播/集群）选择对应的Topic，根据推送/拉取的方式获取消息**
    *   **Group 同组消费者协调工作均衡消费消息，集群模式下一个队列最多对应一个消费者，如果消费者数量超过队列数量则无效**

通过以下的架构图，能够容易理解NameServer、Broker、Product、Consumer集群之间的关系：

1.  **NameServer集群启动**
2.  **Broker集群配置NameServer集群地址并启动，向每个NameServer节点心跳的同时携带自身broker中的Topic、MessageQueue等路由信息(routing info)**
3.  **Product、Consumer客户端配置NameServer集群地址启动后，定时任务获取broker信息（broker上下线等操作也会即时更新）**
4.  **Product根据负载均衡算法、Topic、MessageQueue和Broker信息找到要通信的Broker发送消息**
5.  **Broker收到Product消息后进行持久化**
6.  **Consumer根据再平衡算法得到自己要消费的队列，再通过Broker信息通信获取消息进行消费**

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240822/1724298965185510626.png)

在这个流程中，小菜有个疑问：为什么Product、Consumer获取Broker数据要通过NameServer通信？

1.  **NameServer在架构中的作用如注册中心，管理服务注册与发现**
2.  **架构解耦**，将心跳/交互数据/判断状态等功能交给NameServer（注册中心）去做
3.  **在broker集群下，如果没有NameServer这种broker间需要互相进行心跳和同步汇总的数据，当节点繁多时增加带宽压力，另外broker宕机时还要增加机制来进行判断是否下线，并且product与consumer需要配置broker信息会非常多**（需要网络通信）
4.  **NameServer集群间节点无状态互不通信，提供高可用集群**

#### Spring Boot 快速上手

RocketMQ的broker作为服务端，NameServer作为注册中心，与编写代码的接触比较少，较多的还是生产者与消费者（客户端）

经过大量的理论知识，我们知道MQ的大致流程，接下来使用SpringBoot编写代码实现Product和Consumer客户端

原生RocketMQ提供的生产者与消费者API繁多并且使用时需要try catch、使用起来麻烦，企业级开发通常会在其基础上进行封装常用的API

Spring Boot 框架作为脚手架，整合RocketMQ会非常快，并且还提供对应的RocketMQTemplate对原生API进行封装简化开发

1.  **导入maven依赖**

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

2.  **封装原生Product API**

企业级开发常常会对原生API进行封装，而其中的ServerProduct是自定义的类，组合原生默认的生产者DefaultMQProducer来封装API简化开发

在这个过程中，**通常会用配置文件的方式配置有关生产者的参数如：组名、nameserver地址、发送消息失败重试次数、发送消息超时时间等**...

**设置完参数后，启动生产者 `producer.start()`**

```java
public class ServerProduct {

    private DefaultMQProducer producer;

    public ServerProduct(String producerGroup) {
        producer = new DefaultMQProducer(producerGroup);
        init();
    }

    public ServerProduct() {
        producer = new DefaultMQProducer("Default_Server_Producer_Group");
        init();
    }

    private void init() {
        //初始化 主要通过配置文件的值进行set 最后启动生产者
        producer.setNamesrvAddr("127.0.0.1:9876");
        //...

        try {
            producer.start();
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }
}
```

**启动生产者主要会去启动定时任务同步nameserver、broker数据，并初始化一些组件，后续用于客户端网络通信、负载均衡等**

这些原理放到后文源码解析再具体聊聊\~

然后再封装一个发送消息的API：

sendSyncMsg 发送同步消息API中第一个参数为topic（一级分类），第二个传输为tag（二级分类），第三个传输为消息体

内部会**通过参数构建Message并使用原生API发送给Broker**

```java
public SendResult sendSyncMsg(String topic, String tag, String jsonBody) {
    Message message = new Message(topic, tag, jsonBody.getBytes(StandardCharsets.UTF_8));
    SendResult sendResult;
    try {
        sendResult = producer.send(message);
    } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
        throw new RuntimeException(e);
    }
    return sendResult;
}
```

3.  **编写controller类，使用生产者调用API，发送到broker**

```java
@RequestMapping("/warn")
@RestController
@Slf4j
public class WarnController {

    private static final String topic = "TopicTest";

    @Autowired
    private ServerProduct producer;

    @GetMapping("/syncSend")
    public SendResult syncSend() {
        return producer.sendSyncMsg(topic, "tag", "sync hello world!");
    }
}
```

4.  **消费者订阅Topic**

发送完消息后，消息会持久化到broker中，因此我们需要使用消费者获取消息并进行消费

企业级开发时通常会使用注解的方式标识consumer需要订阅的信息，再通过解析注解的方式将数据注入的消费者中，我们这里直接使用spring提供的注解@RocketMQMessageListener

```java
@Component
@RocketMQMessageListener(topic = "TopicTest", consumerGroup = "warn_consumer_group")
public class WarnConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        // 处理消息
        System.out.println("Received message: " + message);
    }
}
```

5.  **手动创建Topic：TopicTest** 不要自动创建Topic 项目太大可能导致遗忘

使用[Dashboard](https://rocketmq.apache.org/zh/docs/4.x/deployment/03Dashboard)手动创建Topic

6.  **启动NameServer、Broker再启动SpringBoot服务，访问 /warn/syncSend 测试发送消息、消费**

NameServer、Broker的部署可以查看[官方文档 ](https://rocketmq.apache.org/zh/docs/4.x/quickstart/01quickstart)

至此我们经历过消息的生产与消费，但在消息的“一生”中还可能出现各种各样的情况：

1.  发送消息的方式：普通（同步、异步、单向）/顺序/延迟/批量/事务消息等...
2.  消费消息的方式：push/pull消费、集群/广播模式...
3.  如何保证消息不丢失？
4.  消息是如何高效持久化的？
5.  如何保证消费幂等？如何解决消息堆积、延时？

这些情况后文都会由浅入深一一解决，查看RocketMQ实现原理，并从原理中体验出设计的思想，再去查看其他的消息中间件\~

#### 总结

**消息中间件通常有削峰填谷、异步通信、架构解耦、高性能、高可用、集群扩展、负载均衡等相关特点，同时项目中引入消息中间件也会增加调用链路、系统复杂度**

**RocketMQ由NameServer、Broker、Product、Consumer等集群组成**

**其中Broker作为服务端，负责接收消息、对消息进行高效持久化、消费消息时高效查询**

**定义Topic对消息进行分类，为了提升水平扩展写入能力，Topic下可以设置MessageQueue队列，消息作为数据载体存储在队列中，等待被消费**

**为了满足高可用，相同的Topic会被放到不同的master broker，避免”所有坤蛋都在同一个篮子“中**

**NameServer集群作为“注册中心”，节点无状态之间互不通信，只与Broker集群心跳同时更新路由信息，等到Product、Consumer定时通信时再将Broker信息进行传输**

**Product为消息生产方，通过与NameServer获取的Broker中Topic、队列ID等信息，使用负载均衡算法后找到对应Broker进行通信**

**Consumer为消息消费者，根据再平衡负载均衡得到自己负责消费的队列，再通过Broker获取消息进行消费**

#### 最后（点赞、收藏、关注求求啦\~）

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜





## RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）

[上篇文章](https://juejin.cn/post/7411686792342732841)主要介绍消息中间件并以RocketMQ架构展开描述其核心组件以及MQ运行流程

本篇文章以Product的视角来看看发送消息的核心原理与设计思想，最后以图文并茂的方式描述出发送消息的核心流程

### 消息发送方式

**RocketMQ中普通消息提供三种发送方式：同步、异步、单向**

[上篇文章](https://juejin.cn/post/7411686792342732841)中我们已经使用封装好的API延时过同步发送

在使用三种方式前，我们先来理解它们的理论知识

**同步发送：发送完消息后，需要阻塞直到收到Broker的响应，通常用于数据一致性较高的操作，需要确保消息到达Broker并持久化**

**同步发送收到响应并不一定就是成功，还需要根据响应状态进行判断**

SendResult响应状态包括：

1.  SEND\_OK：发送成功
2.  FLUSH\_DISK\_TIMEOUT：刷盘超时
3.  FLUSH\_SLAVE\_TIMEOUT：同步到备超时
4.  SLAVE\_NOT\_AVAILABLE：备不可用

（这些状态与设置的刷盘策略有关，后续保证消息可靠的文章再进行详细展开说明，本篇文章还是回归主线探究发送消息）

**异步发送：发送完消息后立即响应，不需要阻塞等待，但需要设置监听器，当消息成功或失败时进行业务处理，可以在失败时进行重试等其他逻辑保，通常用于追求响应时间的场景**

异步发送相当于同步发送，**需要新增SendCallback回调来进行后续成功/失败的处理，并且异步发送没有返回值**

```java
@GetMapping("/asyncSend")
public String asyncSend() {
    producer.sendAsyncMsg(topic, "tag", "async hello world!", new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            log.info("消息发送成功{}", sendResult);
        }

        @Override
        public void onException(Throwable throwable) {
            log.info("消息发送失败", throwable);
            //记录后续重试
        }
    });
    return "asyncSend ok";
}
```

原生API封装：

```java
public void sendAsyncMsg(String topic, String tag, String jsonBody, SendCallback sendCallback) {
        Message message = new Message(topic, tag, jsonBody.getBytes(StandardCharsets.UTF_8));
        try {
            producer.send(message, sendCallback);
        } catch (MQClientException | RemotingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
}
```

**单向发送：只要发出消息就响应，性能最好，通常用于追求性能，不追求可靠的场景，如：异步日志收集**

由于单向发送的特性，即不需要回调也没有返回结果

```java
@GetMapping("/sendOnewayMsg")
public String onewaySend() {
    producer.sendOnewayMsg(topic, "tag", "oneway hello world!");
    return "sendOnewayMsg ok";
}
```

原生API封装：

```java
public void sendOnewayMsg(String topic, String tag, String jsonBody) {
    Message message = new Message(topic, tag, jsonBody.getBytes(StandardCharsets.UTF_8));
    try {
        producer.sendOneway(message);
    } catch (MQClientException | RemotingException | InterruptedException e) {
        throw new RuntimeException(e);
    }
}
```

### 发送消息原理

在研究发送消息的原理前，不妨来思考下，如果让我们实现，我们要思考下需要哪些步骤？

像我们平时进行业务代码编写前的第一步就是进行参数校验，因为要防止参数“乱填”的意外情况

然后由于需要找到对应的Broker，那肯定要获取Topic路由相关信息

这个路由信息前文说过是从NameServer集群定时获取即时更新的，那么客户端的内存里肯定会进行存储

像这样的数据肯定是类似于多级缓存的，先在本地缓存，如果本地没有或者本地是旧数据，那么就网络通信再去远程（NameServer）获取一份后再更新本地缓存

获取完路由信息后，可以通过设置的Topic获取对应的MessageQueue队列信息，因为Topic下可能有很多队列，因此需要负载均衡算法决定要发送的队列

rocketmq发送消息还提供超时、重试等机制，因此在这个过程中需要计算时间、重试次数

最后发送消息会进行网络通信，我们要选择合适的工具进行RPC

总结一下，如果让我们设计起码要有这些流程：参数校验、获取路由信息、根据负载均衡算法选择队列、计算超时，重试次数、选择网络通信RPC工具...

在设计完流程后，如果我们是一位”成熟的设计师“，那么一定会将这些步骤中通用的步骤抽象成模板，模板可以作为三种发送消息通用方式，而那些变动的就是策略，解耦互不影响，并在重要的流程前后留下”钩子“，方便让使用者进行扩展

rocketmq流程与我们设计、思考的流程类似，先准备一张最终的流程图，方便跟着流程图一起阅读源码：
 ![](https://bbs-img.huaweicloud.com/blogs/img/20240826/1724654474321439845.png)

#### sendDefaultImpl 通用发送消息模板

通过三种发送方式，都会来到`DefaultMQProducerImpl.sendDefaultImpl`这个就是通用方法的模板

代码块中只展示部分关键代码，流程如下：

1.  **参数校验 Validators.checkMessage**
2.  **获取路由信息 tryToFindTopicPublishInfo**
3.  **选择一个要发送的MessageQueue selectOneMessageQueue**
4.  **发送消息 sendKernelImpl**

在3、4步骤中**还会进行重试、超时判断**等

```java
private SendResult sendDefaultImpl(
    //消息
    Message msg,
    //方式
    final CommunicationMode communicationMode,
    //异步的回调
    final SendCallback sendCallback,
    //超时时间
    final long timeout
) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
    //参数校验
    Validators.checkMessage(msg, this.defaultMQProducer);
    
    //获取路由信息
    TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
    if (topicPublishInfo != null && topicPublishInfo.ok()) {
        
        //计算重试次数
        int timesTotal = communicationMode == CommunicationMode.SYNC ? 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed() : 1;
        //已经重发次数
        int times = 0;
        //重试循环
        for (; times < timesTotal; times++) {
            //上次试过的Broker
            String lastBrokerName = null == mq ? null : mq.getBrokerName();
            
            //选择一个要发送的MessageQueue
            MessageQueue mqSelected = this.selectOneMessageQueue(topicPublishInfo, lastBrokerName);
            if (mqSelected != null) {
                mq = mqSelected;
                try {
                    beginTimestampPrev = System.currentTimeMillis();
                    //重发时设置topic
                    if (times > 0) {
                        //Reset topic with namespace during resend.
                        msg.setTopic(this.defaultMQProducer.withNamespace(msg.getTopic()));
                    }
                    //超时退出
                    long costTime = beginTimestampPrev - beginTimestampFirst;
                    if (timeout < costTime) {
                        callTimeout = true;
                        break;
                    }

                    //发送
                    sendResult = this.sendKernelImpl(msg, mq, communicationMode, sendCallback, topicPublishInfo, timeout - costTime);
                    endTimestamp = System.currentTimeMillis();
                    //记录延时
                    this.updateFaultItem(mq.getBrokerName(), endTimestamp - beginTimestampPrev, false);
                    //最后分情况处理
                    switch (communicationMode) {
                        case ASYNC:
                            return null;
                        case ONEWAY:
                            return null;
                        case SYNC:
                            //如果响应状态不成功 如果设置重试其他broker则进行重试
                            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                                if (this.defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()) {
                                    continue;
                                }
                            }
                            return sendResult;
                        default:
                            break;
                    }
                } 
       //...             
}
```

其中CommunicationMode就是发送的方式，分别为：SYNC同步、ASYNC异步、ONEWAY单向

#### tryToFindTopicPublishInfo 获取路由信息

rocketmq中使用大量散列表存储数据，其中存储路由信息的是

```java
ConcurrentMap<String/* topic */, TopicPublishInfo> topicPublishInfoTable = new ConcurrentHashMap<String, TopicPublishInfo>()
```

**topicPublishInfoTable中Key为topic，Value为路由信息TopicPublishInfo**

**TopicPublishInfo中主要包括messageQueueList对应的队列列表、sendWhichQueue后续用来选择哪一个队列、topicRouteData路由数据**

**在topicRouteData路由数据中主要有brokerDatas、queueDatas**

**brokerDatas包含所有的Broker信息，queueDatas包含每个broker上对应的数据，比如读写队列数量**

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240823/1724398494119829928.png)

在获取路由信息的方法中，**先尝试从本地获取 `this.topicPublishInfoTable.get` ，如果本地不存在则从NameServer获取 `this.mQClientFactory.updateTopicRouteInfoFromNameServer`**

（这里的`this.mQClientFactory`实际上是MQClientInstance，生产者、消费者都会用到，用于客户端远程调用服务端，里面也会存对应相关的组件）

```java
private TopicPublishInfo tryToFindTopicPublishInfo(final String topic) {
    //本地获取
    TopicPublishInfo topicPublishInfo = this.topicPublishInfoTable.get(topic);
    if (null == topicPublishInfo || !topicPublishInfo.ok()) {
        //远程获取
        this.topicPublishInfoTable.putIfAbsent(topic, new TopicPublishInfo());
        this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic);
        topicPublishInfo = this.topicPublishInfoTable.get(topic);
    }

    if (topicPublishInfo.isHaveTopicRouterInfo() || topicPublishInfo.ok()) {
        return topicPublishInfo;
    } else {
        //远程获取
        this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic, true, this.defaultMQProducer);
        topicPublishInfo = this.topicPublishInfoTable.get(topic);
        return topicPublishInfo;
    }
}
```

#### selectOneMessageQueue 选择队列

选择队列默认情况下会来到这里，会**通过线性轮询选择队列 selectOneMessageQueue，重试的区别为本次选择的broker不和上次的相同**

（因为上次失败broker可能会存在问题，这次就换一个broker）

```java
public MessageQueue selectOneMessageQueue(final String lastBrokerName) {
    //lastBrokerName：上一次的broker
    if (lastBrokerName == null) {
        //线性轮询选择队列 selectOneMessageQueue
        return selectOneMessageQueue();
    } else {
        for (int i = 0; i < this.messageQueueList.size(); i++) {
            //线性轮询选择队列
            int index = this.sendWhichQueue.incrementAndGet();
            int pos = Math.abs(index) % this.messageQueueList.size();
            if (pos < 0)
                pos = 0;
            MessageQueue mq = this.messageQueueList.get(pos);
            //找到不和上次一样的broker
            if (!mq.getBrokerName().equals(lastBrokerName)) {
                return mq;
            }
        }
        return selectOneMessageQueue();
    }
}
```

#### sendKernelImpl 封装消息

**在发送消息前需要对消息进行封装，如：设置唯一ID、尝试压缩消息、封装消息头等**

**在发送前还有检查禁止发送的钩子和发送前后执行的钩子，方便扩展**

```java
private SendResult sendKernelImpl(final Message msg,
    final MessageQueue mq,
    final CommunicationMode communicationMode,
    final SendCallback sendCallback,
    final TopicPublishInfo topicPublishInfo,
    final long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {

    
    //获取broker信息
    String brokerName = this.mQClientFactory.getBrokerNameFromMessageQueue(mq);
    String brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(brokerName);
    if (null == brokerAddr) {
        tryToFindTopicPublishInfo(mq.getTopic());
        brokerName = this.mQClientFactory.getBrokerNameFromMessageQueue(mq);
        brokerAddr = this.mQClientFactory.findBrokerAddressInPublish(brokerName);
    }

    SendMessageContext context = null;
    if (brokerAddr != null) {
        brokerAddr = MixAll.brokerVIPChannel(this.defaultMQProducer.isSendMessageWithVIPChannel(), brokerAddr);

        byte[] prevBody = msg.getBody();
        try {
            //for MessageBatch,ID has been set in the generating process
            //不是批量消息就设置唯一ID
            if (!(msg instanceof MessageBatch)) {
                MessageClientIDSetter.setUniqID(msg);
            }

            //尝试压缩消息
            int sysFlag = 0;
            boolean msgBodyCompressed = false;
            if (this.tryToCompressMessage(msg)) {
                sysFlag |= MessageSysFlag.COMPRESSED_FLAG;
                sysFlag |= compressType.getCompressionFlag();
                msgBodyCompressed = true;
            }

			//尝试执行检查禁止发送消息的钩子
            if (hasCheckForbiddenHook()) {
                //...
                this.executeCheckForbiddenHook(checkForbiddenContext);
            }

            //尝试执行发送消息前的钩子
            if (this.hasSendMessageHook()) {
                //...
                this.executeSendMessageHookBefore(context);
            }

            //封装消息头
            SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
            //set...

            
            //根据不同的发送方式调整
            SendResult sendResult = null;
            switch (communicationMode) {
                case ASYNC:
                    Message tmpMessage = msg;
                    //...
                    //获取MQ客户端发送
                    sendResult = this.mQClientFactory.getMQClientAPIImpl().sendMessage(
                        brokerAddr,
                        brokerName,
                        tmpMessage,
                        requestHeader,
                        timeout - costTimeAsync,
                        communicationMode,
                        sendCallback,
                        topicPublishInfo,
                        this.mQClientFactory,
                        this.defaultMQProducer.getRetryTimesWhenSendAsyncFailed(),
                        context,
                        this);
                    break;
                case ONEWAY:
                case SYNC:
                    //检查超时
                    long costTimeSync = System.currentTimeMillis() - beginStartTime;
                    if (timeout < costTimeSync) {
                        throw new RemotingTooMuchRequestException("sendKernelImpl call timeout");
                    }
                    //获取MQ客户端发送消息
                    sendResult = this.mQClientFactory.getMQClientAPIImpl().sendMessage(
                        brokerAddr,
                        brokerName,
                        msg,
                        requestHeader,
                        timeout - costTimeSync,
                        communicationMode,
                        context,
                        this);
                    break;
                default:
                    assert false;
                    break;
            }

            //尝试执行发送完消息的钩子
            if (this.hasSendMessageHook()) {
                context.setSendResult(sendResult);
                this.executeSendMessageHookAfter(context);
            }

            return sendResult;
        } 
        //...
}
```

#### 使用Netty进行网络通信RPC

同步消息最终会调用`invokeSync`，这种服务间的网络通信又称为远程调用RPC

在RPC前后也有钩子可以进行扩展

最终调用`invokeSyncImpl`会通过netty的channel进行写数据

```java
public RemotingCommand invokeSync(String addr, final RemotingCommand request, long timeoutMillis)
    throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
    long beginStartTime = System.currentTimeMillis();
    final Channel channel = this.getAndCreateChannel(addr);
    if (channel != null && channel.isActive()) {
            //rpc前的钩子
            doBeforeRpcHooks(addr, request);
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeoutMillis < costTime) {
                throw new RemotingTimeoutException("invokeSync call the addr[" + addr + "] timeout");
            }
        	//使用netty的channel写数据
            RemotingCommand response = this.invokeSyncImpl(channel, request, timeoutMillis - costTime);
        	//rpc后的钩子
            doAfterRpcHooks(RemotingHelper.parseChannelRemoteAddr(channel), request, response);
            this.updateChannelLastResponseTime(addr);
            return response;
    } 
}
```

**通过netty的channel写请求，并添加监听器，最后使用结果调用`waitResponse`进行同步等待**

```java
public RemotingCommand invokeSyncImpl(final Channel channel, final RemotingCommand request,
    final long timeoutMillis)
    throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
    try {
        //结果
        final ResponseFuture responseFuture = new ResponseFuture(channel, opaque, timeoutMillis, null, null);
        this.responseTable.put(opaque, responseFuture);
        final SocketAddress addr = channel.remoteAddress();
        //写请求 并添加监听器
        channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
            //...
        });

        //同步调用 等待结果
        RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMillis);
        return responseCommand;
    } finally {
        this.responseTable.remove(opaque);
    }
}
```

异步消息RPC类似，只是不需要最后的同步等待

#### 重试机制

走完整体的发送消息流程，我们再回过头来查看重试机制

总共尝试发送消息的次数取决于 `int timesTotal = communicationMode == CommunicationMode.SYNC ? 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed() : 1`

如果是同步消息则为 1 + retryTimesWhenSendFailed 默认2次 = 3次，其他情况就是1次

也就是说只有同步发送才会重试！异步、单向都不会进行重试？

就在我查找同步最大重试次数 `retryTimesWhenSendFailed ` 时，同时还发现异步的最大重试次数 `retryTimesWhenSendAsyncFailed`

实际上异步发送重试的代码在异常的catch块中，异常才去执行 `onExceptionImpl`

如果重试同步发送时，需要去其他broker还要把 `retryAnotherBrokerWhenNotStoreOK` 设置为true，默认false

#### 发送消息流程总结

至此发送消息的流程算是过了一遍，在查看源码的过程中大部分内容都是见名知意的，这不比公司的”shit mountain“看着舒服？

最后再来总结下流程，便于同学们记忆：

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240826/1724654474321439845.png)

1.  **先校验参数，避免参数出错**
2.  **再获取Topic路由信息，如果本地没有就从NameServer获取**
3.  **然后通过线性轮询法选择队列，如果`retryAnotherBrokerWhenNotStoreOK` 开启后，同步失败新的重试会选择其他broker**
4.  **紧接着对消息进行封装，设置唯一ID、压缩消息、检查禁止发送钩子、发送前后钩子等**
5.  **最后使用Netty写请求进行rpc，期间也会有rpc的钩子，如果是同步则会等待**
6.  **在此期间会进行重试、超时检测**

### 总结

**消息发送的方式有三种：同步、异步、单向，根据顺序可靠性逐渐下降、性能逐渐提升**

**同步消息能够通过响应判断是否真正成功，常用于需要消息可靠、数据一致的场景，如同步**

**异步消息通过实现回调处理成功与失败，常用于响应时间敏感的场景，如异步短信**

**单向消息不需要进行处理，常用于追求性能的场景，如异步日志**

**消息发送的过程中会先检查消息参数，确保消息无误，再获取路由信息，如果本地不存在则向NameServer获取**

**路由信息存储topic对应的broker、队列列表、broker上的队列等相关信息**

**然后通过线性轮询算法选择要发送消息的队列，如果重试则不会选择相同的broker**

**接着会设置消息的唯一ID、判断是否压缩消息、尝试执行检查禁止发送、发送消息前后的钩子等**

**最后使用netty写请求进行rpc调用，同时也会有rpc前后的钩子**

**在此期间同步、异步会根据参数进行超时检查、重试等操作**

#### 最后（点赞、收藏、关注求求啦\~）

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜





## RocketMQ（三）：面对高并发请求，如何高效持久化消息？

[上篇文章](https://juejin.cn/post/7412672656363798591)我们分析完RocketMQ发送消息的原理，得到结果客户端会通过RPC组件向Broker进行通信

Broker收到请求后需要将消息进行持久化，一旦涉及到持久化，服务器的性能会急速降低，并且消费者进行消费时还需要读取消息，从磁盘的读取也是会大大降低性能的

本篇文章就要来分析，RocketMQ的Broker在这种频繁读写的场景，是如何进行高效读写的

### 存储文件

Broker这种频繁读写的场景中，**提供三种文件满足持久化消息时的高效读写，分别为CommitLog、ConsumerQueue、IndexFile**

#### CommitLog

**为了避免消息持久化时的写入性能开销过大，Broker采用顺序写入，无论消息属于哪个Topic又或者是哪个队列，都顺序写入CommitLog文件**

**CommitLog文件目录下以起始偏移量进行命名，每个文件固定为1G**，从`00000000000000000000`文件开始，接着是`00000000001073741824`，然后是`00000000002147483648`文件，后续以此类推

这些以偏移量命名的文件在源码中被定义为`MappedFile`，从名字可以看出它使用内存映射mmap，**在频繁读写、大文件的场景，使用mmap避免数据拷贝的性能开销**

![](https://bbs-img.huaweicloud.com/blogs/img/20240828/1724822798184890915.png)

由于消息大小不一，持久化到`MappedFile`时每条消息偏移量也不是固定的

**`MappedFile`的创建是需要扩容时才创建的，一连串的`MappedFile`组成CommitLog文件**

#### ConsumerQueue

虽然顺序写能够大大节省写入的开销，但是并不方便查询，因为消息被写顺序写入CommitLog时，并没有区分是哪个Topic、队列的

这样在进行消费时要读取消息，根据topic、队列、队列上的偏移量来获取消息时，遍历查找是不现实的，因此还要生成逻辑文件，便于进行查找

**ConsumerQueue(消费队列)，以Topic进行一级分类，然后再以Topic下的队列ID进行二级分类，队列下的每个文件(固定大小6,000,000 B)可以存储30W条ConsumerQueue记录（20 B）**

并且命名也是以起始偏移量进行命名，比如`00000000000000000000`,`00000000000006000000`,`00000000000012000000`

**ConsumerQueue记录固定大小为20B，其中8B记录对应消息在CommitLog上的偏移量、4B记录消息长度、8B记录tag哈希，其中依靠前两个字段可以快速找到CommitLog中的消息**

ComsumerQueue与CommitLog文件的关系，类比MySQL数据库中的二级索引与聚簇索引

通过二级索引找到满足条件的记录后，回表快速定位到聚簇索引上的记录（如果不理解MySQL索引的同学可以查看MySQL进阶专栏）

**最后一个字段tag哈希用于消息过滤，消费者拉取指定tag消息时，如果哈希不满足就不会进行拉取**

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240828/1724826311524326515.png)

在图中ConsumerQueue文件以Topic进行分类，分为Topic A、B两个文件，其中TopicA下根据队列ID存在0、1、2三个文件

文件中存储的记录由commitlog offset消息在commitlog的偏移量、size消息大小和tag哈希值组成

消费者组中的消费者A向队列0、1拉取消息，消费者B向队列2拉取消息

拉取消息时Broker根据ConsumerQueue记录的commitlog offset可以在CommitLog文件中找到需要的消息

需要注意的是，**CommitLog与ConsumerQueue文件虽然占用空间不同，但底层都是使用MappedFile的，一个MappedFile相当于一个文件**

#### IndexFile

IndexFile文件为哈希索引，文件分为三个部分：**文件头、哈希槽、索引项**

**文件头用于存储通用数据并固定为40B**

```java
public void load() {
    //最早消息存储到commitlog时间 8B 用来计算时间差
    this.beginTimestamp.set(byteBuffer.getLong(beginTimestampIndex));
    //最晚时间 8B
    this.endTimestamp.set(byteBuffer.getLong(endTimestampIndex));
    //消息存储到CommitLog最小偏移量 8B
    this.beginPhyOffset.set(byteBuffer.getLong(beginPhyoffsetIndex));
    //消息存储到CommitLog最大偏移量 8B
    this.endPhyOffset.set(byteBuffer.getLong(endPhyoffsetIndex));
	//最大可以存储的哈希槽数量
    this.hashSlotCount.set(byteBuffer.getInt(hashSlotcountIndex));
    //以使用的索引数量
    this.indexCount.set(byteBuffer.getInt(indexCountIndex));
    //从1开始
    if (this.indexCount.get() <= 0) {
    	this.indexCount.set(1);
    }
}
```

**哈希槽每个占用4B，用来寻址，能够找到索引项**

**索引项每个占用20B，4B存储哈希值、8B存储消息在CommitLog中的偏移量、4B存储与前一个消息持久化的时间差（单位秒，用来时间范围查询）、4B存储下个索引项的偏移量，用于寻址**（哈希冲突时链地址法）

![](https://bbs-img.huaweicloud.com/blogs/img/20240902/1725257512750401743.png)

文件大小 = 文件头 40B + 哈希槽数量 \* 哈希槽固定大小 4B + 索引数量 \* 索引固定大小 4B

```java
int fileTotalSize =  IndexHeader.INDEX_HEADER_SIZE + (hashSlotNum * hashSlotSize) + (indexNum * indexSize);
```

使用流程与哈希表类似：

1.  **通过`topic + # + key`的形式来计算哈希值，哈希值模上哈希槽的数量就找到对应的哈希槽**
2.  **通过哈希槽找到对应的索引项，对比哈希值**
3.  **哈希值相同则获取偏移量，再去CommitLog寻找**
4.  **哈希值不相同则根据联表向后查找下一个索引项**

Broker存储消息的流程中除了CommitLog、ConsumerQueue、IndexFile文件外，还会存储其他文件，后文用到了再说，最终Broker存储的架构图如下：

![](https://bbs-img.huaweicloud.com/blogs/img/20240902/1725256096837588595.png)



### 数据刷盘与同步

在分析原理前，再来说下数据刷盘和同步的几种方式：

**数据刷盘：将操作系统内核缓冲区中的数据刷入磁盘**

**刷盘可以分为同步、异步两种方式，默认异步：**

1.  **同步：向内核缓冲区写完数据后开始等待，直到对应的脏页刷入磁盘再返回；性能差，但可靠性好**
2.  **异步：向内核缓冲区写完数据后立即返回，根据频率、页数等配置将脏页数据刷入磁盘；性能好，但可靠性差**

**数据同步：主Broker持久化数据后，再将数据同步给从Broker，也叫主从复制**

**主从复制也分为同步、异步两种方式，默认异步：**

1.  **同步：向内核缓冲区写完数据后开始等待，直到从broker也持久化完数据；性能差，但可靠性好**
2.  **异步：向内核缓冲区写完数据后直接返回，异步线程对从broker进行数据同步**

后文将会从源码的角度进行分析

### 存储原理

因为上篇分析过生产者发送消息流程的原理，介绍完存储文件后，我们从Broker接收消息的源码开始分析，正好接上原理分析的流程

（Broker的源码去GitHub拉下来查看，如果要跑起来，还要配置Broker的一些参数）

#### Netty服务器接收请求

上篇文章发送消息RPC时说过RocketMQ网络通信使用的Netty框架，那么Broker作为服务端一定会有Netty的服务器

在这个Netty服务器中pipeline可以配置一些处理器用于处理请求、响应

> NettyRemotingServer.start

在启动的流程中，会将NettyServerHandler加入pipeline处理请求

```java
	ServerBootstrap childHandler =
    this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
        .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, nettyServerConfig.getServerSocketBacklog())
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, false)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                    .addLast(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME, handshakeHandler)
                    .addLast(defaultEventExecutorGroup,
                        encoder,
                        new NettyDecoder(),
                        new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()),
                        connectionManageHandler,
                        //NettyServerHandler 加入 pipeline     
                        serverHandler
                    );
            }
        });
```

不认识netty的同学也不要慌，就当它是个网络通信的组件

NettyServerHandler处理请求时会调用`processMessageReceived`方法

```java
class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        processMessageReceived(ctx, msg);
    }
}
```

`processMessageReceived`方法会根据类型分请求/响应两种情况进行处理

```java
public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
    final RemotingCommand cmd = msg;
    if (cmd != null) {
        switch (cmd.getType()) {
            case REQUEST_COMMAND:
                processRequestCommand(ctx, cmd);
                break;
            case RESPONSE_COMMAND:
                processResponseCommand(ctx, cmd);
                break;
            default:
                break;
        }
    }
}
```

调用`processRequestCommand`方法，会先通过请求的类型获取Pair

**Pair中存在NettyRequestProcessor（处理请求）和ExecutorService（对应的线程池）**

然后将任务提交到线程池，任务代码我先省略，待会查看，如果不支持请求类型会直接响应

```java
public void processRequestCommand(final ChannelHandlerContext ctx, final RemotingCommand cmd) {
    //通过请求code获取Pair，NettyRequestProcessor为处理请求，ExecutorService为对应的线程池
    final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
    final Pair<NettyRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
    final int opaque = cmd.getOpaque();

    if (pair != null) {
        //任务..

        try {
            //将任务提交到线程池
            final RequestTask requestTask = new RequestTask(run, ctx.channel(), cmd);
            pair.getObject2().submit(requestTask);
        } catch (RejectedExecutionException e) {
			//...
        }
    } else {
        //响应 请求类型不支持处理
        String error = " request type " + cmd.getCode() + " not supported";
        final RemotingCommand response =
            RemotingCommand.createResponseCommand(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, error);
        response.setOpaque(opaque);
        ctx.writeAndFlush(response);
    }
}
```

任务中主要做几件事：

1.  **解析请求地址**
2.  **执行处理前的钩子**
3.  **封装异步回调，包括：执行处理后的钩子和写回响应**
4.  **调用NettyRequestProcessor处理器的处理方法**，如果是异步就携带回调，如果不是则执行完再调用下回调（流程类似）

```java
Runnable run = new Runnable() {
    @Override
    public void run() {
        try {
            //解析请求地址
            String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            //rpc处理前的钩子
            doBeforeRpcHooks(remoteAddr, cmd);
            
            //异步回调 处理后的钩子和写响应
            final RemotingResponseCallback callback = new RemotingResponseCallback() {
                @Override
                public void callback(RemotingCommand response) {
                    //处理后的钩子
                    doAfterRpcHooks(remoteAddr, cmd, response);
                    if (!cmd.isOnewayRPC()) {
                        if (response != null) {
                            response.setOpaque(opaque);
                            response.markResponseType();
                            response.setSerializeTypeCurrentRPC(cmd.getSerializeTypeCurrentRPC());
                            try {
                                //写响应
                                ctx.writeAndFlush(response);
                            } catch (Throwable e) {
                                //...
                            }
                        } 
                    }
                }
            };
            //调用处理器的方法
            if (pair.getObject1() instanceof AsyncNettyRequestProcessor) {
                AsyncNettyRequestProcessor processor = (AsyncNettyRequestProcessor)pair.getObject1();
                processor.asyncProcessRequest(ctx, cmd, callback);
            } else {
                NettyRequestProcessor processor = pair.getObject1();
                RemotingCommand response = processor.processRequest(ctx, cmd);
                callback.callback(response);
            }
        } catch (Throwable e) {
            //...
        }
    }
};
```

#### SendMessageProcessor 处理请求

```java
public class SendMessageProcessor extends AbstractSendMessageProcessor implements NettyRequestProcessor {
}

public abstract class AbstractSendMessageProcessor extends AsyncNettyRequestProcessor implements NettyRequestProcessor {
}
```

处理请求的处理器SendMessageProcessor是异步的，因此会调用`asyncProcessRequest`方法

```java
@Override
public void asyncProcessRequest(ChannelHandlerContext ctx, RemotingCommand request, RemotingResponseCallback responseCallback) throws Exception {
    asyncProcessRequest(ctx, request).
        thenAcceptAsync(responseCallback::callback, this.brokerController.getPutMessageFutureExecutor());
}
```

`asyncProcessRequest` 处理请求分成两种情况：

1.  消费者发送的，调用 `asyncConsumerSendMsgBack`
2.  剩下的情况就是生产者发送的消息，需要解析请求头、执行处理发送消息前的钩子、查看是否批量消息（分情况处理）

```java
public CompletableFuture<RemotingCommand> asyncProcessRequest(ChannelHandlerContext ctx,
                                                              RemotingCommand request) throws RemotingCommandException {
    final SendMessageContext mqtraceContext;
    switch (request.getCode()) {
        case RequestCode.CONSUMER_SEND_MSG_BACK:
            return this.asyncConsumerSendMsgBack(ctx, request);
        default:
            //解析请求头
            SendMessageRequestHeader requestHeader = parseRequestHeader(request);
            if (requestHeader == null) {
                return CompletableFuture.completedFuture(null);
            }
            mqtraceContext = buildMsgContext(ctx, requestHeader);
            //执行处理发送消息前的钩子
            this.executeSendMessageHookBefore(ctx, request, mqtraceContext);
            //是否批量消息 分情况处理
            if (requestHeader.isBatch()) {
                return this.asyncSendBatchMessage(ctx, request, mqtraceContext, requestHeader);
            } else {
                return this.asyncSendMessage(ctx, request, mqtraceContext, requestHeader);
            }
    }
}
```

查看普通消息的情况，调用 `asyncSendMessage`，才开始真正处理发送消息的请求：

1.  **获取消息体、队列ID、Topic等信息，将数据注入MessageExtBrokerInner**
2.  **是否为事务消息，使用MessageExtBrokerInner后续执行**

```java
private CompletableFuture<RemotingCommand> asyncSendMessage(
    ChannelHandlerContext ctx, RemotingCommand request,
    SendMessageContext mqtraceContext,SendMessageRequestHeader requestHeader) {
    
    //生成通用响应
    final RemotingCommand response = preSend(ctx, request, requestHeader);
    final SendMessageResponseHeader responseHeader = (SendMessageResponseHeader)response.readCustomHeader();

    if (response.getCode() != -1) {
        return CompletableFuture.completedFuture(response);
    }

    //获取消息体、队列ID、Topic等信息
    final byte[] body = request.getBody();
    int queueIdInt = requestHeader.getQueueId();
    TopicConfig topicConfig = this.brokerController.getTopicConfigManager().selectTopicConfig(requestHeader.getTopic());

    //将信息封装到MessageExtBrokerInner
    MessageExtBrokerInner msgInner = new MessageExtBrokerInner();
    msgInner.setTopic(requestHeader.getTopic());
    msgInner.setQueueId(queueIdInt);
    msgInner.setBody(body);
    //set...
    
    CompletableFuture<PutMessageResult> putMessageResult = null;
    //分情况处理，一个是事务消息，另一个是普通消息
    String transFlag = origProps.get(MessageConst.PROPERTY_TRANSACTION_PREPARED);
    if (Boolean.parseBoolean(transFlag)) {
        putMessageResult = this.brokerController.getTransactionalMessageService().asyncPrepareMessage(msgInner);
    } else {
        putMessageResult = this.brokerController.getMessageStore().asyncPutMessage(msgInner);
    }
    //写响应
    return handlePutMessageResultFuture(putMessageResult, response, request, msgInner, responseHeader, mqtraceContext, ctx, queueIdInt);
}
```

继续查看普通消息的后续流程，`this.brokerController.getMessageStore().asyncPutMessage(msgInner)`

`getMessageStore`会获取消息存储的组件MessageStore进行后续的存储流程

#### MessageStore 存储消息

MessageStore负责消息的存储，除了写CommitLog文件外，还要去写ConsumerQueue、IndexFile等文件

采用DefaultMessageStore处理消息：

（我们先把写CommitLog的主流程继续走完）

1.  **先检查存储、消息、本地MQ是否正常**，这里的本地MQ并不是指RocketMQ，而是本地内存MQ
2.  **调用CommitLog处理消息**（历尽千辛万苦终于看到熟悉的CommitLog了\~）
3.  处理完统计耗时、失败次数等

```java
@Override
public CompletableFuture<PutMessageResult> asyncPutMessage(MessageExtBrokerInner msg) {
    //检查存储、消息、本地MQ
    PutMessageStatus checkStoreStatus = this.checkStoreStatus();
    PutMessageStatus msgCheckStatus = this.checkMessage(msg);
    PutMessageStatus lmqMsgCheckStatus = this.checkLmqMessage(msg);
	//...
    
    long beginTime = this.getSystemClock().now();
    //调用CommitLog处理消息
    CompletableFuture<PutMessageResult> putResultFuture = this.commitLog.asyncPutMessage(msg);
	
    //处理后要统计下耗时，在哪个时间段，累计失败次数等
    putResultFuture.thenAccept(result -> {
        long elapsedTime = this.getSystemClock().now() - beginTime;
        if (elapsedTime > 500) {
            log.warn("putMessage not in lock elapsed time(ms)={}, bodyLength={}", elapsedTime, msg.getBody().length);
        }
        this.storeStatsService.setPutMessageEntireTimeMax(elapsedTime);

        if (null == result || !result.isOk()) {
            this.storeStatsService.getPutMessageFailedTimes().add(1);
        }
    });

    return putResultFuture;
}
```

##### 写CommitLog

###### CommitLog追加消息

> CommitLog.asyncPutMessage

终于到达CommitLog存储消息的流程，这里的流程还是比较重要的：

1.  **写前加锁** （由于会顺序写同一个文件，那一定会出现并发写数据的情况，因此要加锁）
2.  **获取最后一个MappedFile，没有就创建** （MappedFile就是对应以偏移量命名的文件，第一个文件不一定为`00000000000000000000`，因为消费完会删除，第一个文件为偏移量最小的）
3.  **使用MappedFile追加消息**
4.  **处理结果，写后释放锁**
5.  **提交刷盘请求** （mmap只是将数据写到page cache，还需要根据同步/异步刷盘策略再进行刷盘）
6.  **提交主从复制的请求** （也是同步/异步的策略进行主从复制）
7.  **刷盘、主从复制请求完成后才返回**

```java
public CompletableFuture<PutMessageResult> asyncPutMessage(final MessageExtBrokerInner msg) {
        //处理事务消息 省略...

        //...
    
    	//加锁 自旋锁或可重入锁
        putMessageLock.lock(); 
        try {
            //获取mappedFileQueue队列中最后一个MappedFile
            MappedFile mappedFile = this.mappedFileQueue.getLastMappedFile();
            
            //mappedFile为空要创建
            if (null == mappedFile || mappedFile.isFull()) {
                mappedFile = this.mappedFileQueue.getLastMappedFile(0); // Mark: NewFile may be cause noise
            }
            
			//mappedFile追加消息
            result = mappedFile.appendMessage(msg, this.appendMessageCallback, putMessageContext);
            
            //处理结果
            switch (result.getStatus()) {
                //...
            }

            elapsedTimeInLock = this.defaultMessageStore.getSystemClock().now() - beginLockTimestamp;
        } finally {
            beginTimeInLock = 0;
            //释放锁
            putMessageLock.unlock();
        }

    	//...
    
        //提交刷盘请求
        CompletableFuture<PutMessageStatus> flushResultFuture = submitFlushRequest(result, msg);
    	//提交传输从节点数据请求
        CompletableFuture<PutMessageStatus> replicaResultFuture = submitReplicaRequest(result, msg);
    
    	//都提交后返回
        return flushResultFuture.thenCombine(replicaResultFuture, (flushStatus, replicaStatus) -> {
            if (flushStatus != PutMessageStatus.PUT_OK) {
                putMessageResult.setPutMessageStatus(flushStatus);
            }
            if (replicaStatus != PutMessageStatus.PUT_OK) {
                putMessageResult.setPutMessageStatus(replicaStatus);
            }
            return putMessageResult;
        });
    }
```

mappedFile追加消息就是计算偏移量，再将数据写入缓冲区

###### 创建MappedFile

CommitLog追加消息的流程中，如果MappedFile不存在，则会创建，最终调用`doCreateMappedFile`

```java
protected MappedFile doCreateMappedFile(String nextFilePath, String nextNextFilePath) {
    MappedFile mappedFile = null;

    //采用allocateMappedFileService创建
    if (this.allocateMappedFileService != null) {
        mappedFile = this.allocateMappedFileService.putRequestAndReturnMappedFile(nextFilePath,
                nextNextFilePath, this.mappedFileSize);
    } else {
        //否则自己创建
        try {
            mappedFile = new MappedFile(nextFilePath, this.mappedFileSize);
        } catch (IOException e) {
            log.error("create mappedFile exception", e);
        }
    }

    return mappedFile;
}
```

**`AllocateMappedFileService`是负责创建MappedFile的组件，会将请求放入内存队列`this.requestQueue.offer`后续由它的线程取出请求进行创建**

###### 刷盘

追加完消息后会提交两个请求：

**`submitFlushRequest`提交刷盘请求，主要分两种情况：同步、异步**

**`submitReplicaRequest`提交主从复制请求，也是分为同步情况与刷盘类似，只是增加HA的组件**

```java
public CompletableFuture<PutMessageStatus> submitFlushRequest(AppendMessageResult result, MessageExt messageExt) {
    // Synchronization flush 同步
    if (FlushDiskType.SYNC_FLUSH == this.defaultMessageStore.getMessageStoreConfig().getFlushDiskType()) {
        final GroupCommitService service = (GroupCommitService) this.flushCommitLogService;
        if (messageExt.isWaitStoreMsgOK()) {
            GroupCommitRequest request = new GroupCommitRequest(result.getWroteOffset() + result.getWroteBytes(),
                    this.defaultMessageStore.getMessageStoreConfig().getSyncFlushTimeout());
            flushDiskWatcher.add(request);
            //GroupCommitService添加请求
            service.putRequest(request);
            return request.future();
        } else {
            service.wakeup();
            return CompletableFuture.completedFuture(PutMessageStatus.PUT_OK);
        }
    }
    // Asynchronous flush
    else {
        if (!this.defaultMessageStore.getMessageStoreConfig().isTransientStorePoolEnable()) {
            flushCommitLogService.wakeup();
        } else  {
            //异步唤醒
            commitLogService.wakeup();
        }
        return CompletableFuture.completedFuture(PutMessageStatus.PUT_OK);
    }
}
```

**如果是同步，则会使用`GroupCommitService`进行同步刷盘，将请求放入它的队列**

**如果是异步，则唤醒`FlushRealTimeService`进行刷盘**

**默认情况下，刷盘策略为异步**

返回后，由外层的 `DefaultMessageStore`的 `waitForPutResult` 进行阻塞等待结果

```java
@Override
public PutMessageResult putMessage(MessageExtBrokerInner msg) {
    return waitForPutResult(asyncPutMessage(msg));
}
```

###### 同步刷盘

**`CommitLog` 在构造时根据字段 `flushDiskType` 判断刷盘策略是同步还是异步（默认异步）**

**同步使用GroupCommitService，异步使用FlushRealTimeService**

```java
public CommitLog(final DefaultMessageStore defaultMessageStore) {
    //...
    
    if (FlushDiskType.SYNC_FLUSH == defaultMessageStore.getMessageStoreConfig().getFlushDiskType()) {
        this.flushCommitLogService = new GroupCommitService();
    } else {
        this.flushCommitLogService = new FlushRealTimeService();
    }
}
```

GroupCommitService负责同步刷盘，提供两个队列

requestsWrite负责写，提交刷盘任务时会被放入队列中

requestsRead负责读，每次刷盘时取出请求进行刷盘

```java
private volatile LinkedList<GroupCommitRequest> requestsWrite = new LinkedList<GroupCommitRequest>();
private volatile LinkedList<GroupCommitRequest> requestsRead = new LinkedList<GroupCommitRequest>();
```

GroupCommitService 会轮询取出请求进行刷盘

```java
public void run() {

    while (!this.isStopped()) {
        try {
            //等待 其中会交换队列
            this.waitForRunning(10);
            //取出请求 刷盘
            this.doCommit();
        } catch (Exception e) {
            CommitLog.log.warn(this.getServiceName() + " service has exception. ", e);
        }
    }

    // Under normal circumstances shutdown, wait for the arrival of the
    // request, and then flush
    try {
        Thread.sleep(10);
    } catch (InterruptedException e) {
        CommitLog.log.warn(this.getServiceName() + " Exception, ", e);
    }

    //结束前再刷盘兜底
    synchronized (this) {
        this.swapRequests();
    }
    this.doCommit();
}
```

`waitForRunning` 等待期间会调用交互请求队列，将读写队列互换

```java
private void swapRequests() {
    lock.lock();
    try {
        LinkedList<GroupCommitRequest> tmp = this.requestsWrite;
        this.requestsWrite = this.requestsRead;
        this.requestsRead = tmp;
    } finally {
        lock.unlock();
    }
}
```

`doCommit` 会遍历读队列进行刷盘，**通过比较偏移量判断是否刷盘成功，刷盘可能涉及两个mappedFile，因此可能循环两次**

```java
private void doCommit() {
    if (!this.requestsRead.isEmpty()) {
        for (GroupCommitRequest req : this.requestsRead) {
            // There may be a message in the next file, so a maximum of
            // two times the flush
            //通过比较偏移量判断是否刷盘成功
            boolean flushOK = CommitLog.this.mappedFileQueue.getFlushedWhere() >= req.getNextOffset();
            //刷盘可能涉及两个mappedFile因此可能要循环两次
            for (int i = 0; i < 2 && !flushOK; i++) {
                CommitLog.this.mappedFileQueue.flush(0);
                flushOK = CommitLog.this.mappedFileQueue.getFlushedWhere() >= req.getNextOffset();
            }

            //填充结果：成功 或 刷盘超时
            req.wakeupCustomer(flushOK ? PutMessageStatus.PUT_OK : PutMessageStatus.FLUSH_DISK_TIMEOUT);
        }

        long storeTimestamp = CommitLog.this.mappedFileQueue.getStoreTimestamp();
        if (storeTimestamp > 0) {
            CommitLog.this.defaultMessageStore.getStoreCheckpoint().setPhysicMsgTimestamp(storeTimestamp);
        }
        this.requestsRead = new LinkedList<>();
    } else {
        // Because of individual messages is set to not sync flush, it
        // will come to this process
        // 读队列没数据的情况下直接刷
        CommitLog.this.mappedFileQueue.flush(0);
    }
}
```

其中 `flush(0)` 表示刷盘没有最低页数的要求，会尽力刷盘

**最终调用 `force` 进行刷盘，`fileChannel` 是文件的channel，`mappedByteBuffer` 是它的直接内存缓冲区**

```java
if (writeBuffer != null || this.fileChannel.position() != 0) {
    this.fileChannel.force(false);
} else {
    this.mappedByteBuffer.force();
}
```

值得思考的是读写队列都采用线程不安全的`LinkedList`，在并发提交刷盘任务的情况下需要加锁同步，那为啥不使用JUC下的队列呢？

也许是因为刷盘读队列消费时为单线程并不需要使用同步手段，最终才选择`LinkedList`

###### 异步刷盘

FlushRealTimeService 负责异步刷盘

**当使用异步刷盘时，也是通过轮询刷盘，可以通过配置参数调整刷盘频率、每次刷盘最少的页数等**

```java
public void run() {
    CommitLog.log.info(this.getServiceName() + " service started");

    while (!this.isStopped()) {
        //是否使用Thread.sleep 默认true
        boolean flushCommitLogTimed = CommitLog.this.defaultMessageStore.getMessageStoreConfig().isFlushCommitLogTimed();
		//刷盘频率 默认500ms
        int interval = CommitLog.this.defaultMessageStore.getMessageStoreConfig().getFlushIntervalCommitLog();
        //每次最少刷屏页数 默认4页
        int flushPhysicQueueLeastPages = CommitLog.this.defaultMessageStore.getMessageStoreConfig().getFlushCommitLogLeastPages();
		
        //...

        try {
            //等待 刷盘频率的时间
            if (flushCommitLogTimed) {
                Thread.sleep(interval);
            } else {
                this.waitForRunning(interval);
            }
            //刷盘
            CommitLog.this.mappedFileQueue.flush(flushPhysicQueueLeastPages);
            //...
        } catch (Throwable e) {
            CommitLog.log.warn(this.getServiceName() + " service has exception. ", e);
            this.printFlushProgress();
        }
    }

    // Normal shutdown, to ensure that all the flush before exit
    //结束前 兜底刷盘
    boolean result = false;
    for (int i = 0; i < RETRY_TIMES_OVER && !result; i++) {
        result = CommitLog.this.mappedFileQueue.flush(0);
        CommitLog.log.info(this.getServiceName() + " service shutdown, retry " + (i + 1) + " times " + (result ? "OK" : "Not OK"));
    }

    this.printFlushProgress();

    CommitLog.log.info(this.getServiceName() + " service end");
}
```

**与同步刷盘不同：刷盘频率很少（等待时间久）、每次刷盘并不是尽力刷而是根据最少页数进行刷盘**

###### 小结

至此CommitLog文件被刷入磁盘，主从复制的“支线”后续文章再来分析

由于消息写入CommitLog流程漫长、复杂，我们总结核心流程，简化流程：

1.  **Netty服务器接收请求**
2.  **SendMessageProcessor将请求转化为MessageExtBrokerInner （Broker内部处理的消息）**
3.  **MessageStore 检查消息、存储状态，调用CommitLog持久化**
4.  **CommitLog 写前加锁防并发写**
5.  **CommitLog 没有MappedFile或者已满，将请求放入内存队列，通过AllocateMappedFileService异步创建**
6.  **CommitLog 使用最后一个MappedFile，追加数据，最终通过fileChannel或它的缓冲区mappedByteBuffer进行force刷盘**
7.  **CommitLog 写完释放锁**
8.  **CommitLog 提交刷盘请求：默认异步刷盘根据配置的频率和每次刷盘页数进行刷盘，同步刷盘会将请求提交到写队列，循环消费，消费前置换读写队列，取出请求尽量刷盘**
9.  **CommitLog 提交主从复制请求**
10.  **MessageStore 刷盘、主从复制请求完成后返回**

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240830/1724982584853543531.png)

再精简下流程：`Netty -> SendMessageProcessor -> MessageStore -> CommitLog -> MappedFile -> fileChannel/mappedByteBuffer force`

当消费者获取消息后，CommitLog中存储的消息就不需要保存了，因此会有清理线程来进行定时清理

实现定时清理的组件是MessageStore下的CleanCommitLogService，实际上就是操作MappedFile，销毁关闭资源，这里就不过多叙述

##### 写ConsumerQueue

整理完“主线”，消息持久化并没有结束，还有部分的“支线”要挖掘

（就像黑神话悟空一样，要把图舔干净才有美妙的探索感\~）

与CommitLog文件对标的ConsumerQueue、IndexFile文件似乎没有出现主流程的源码中，它们是啥时候生成的？接下来让我们继续分析：

###### ReputMessageService 重投消息

DefaultMessageStore下的\*\*`ReputMessageService`用于重投消息，它会根据CommitLog上的偏移量封装成请求，重投给其他`CommitLogDispatcher`进行后续处理\*\*

常见的`CommitLogDispatcher`有写ConsumerQueue的`CommitLogDispatcherBuildConsumeQueue`、写IndexFile的`CommitLogDispatcherBuildIndex`

线程循环执行，每次循环等待1ms，然后调用`doReput`，其中reputFromOffset为它记录的偏移量

1.  **根据重投偏移量获取CommitLog（封装为SelectMappedBufferResult）**
2.  **然后每次循环检查、解析每条消息，封装为DispatchRequest**
3.  **如果成功就使用对应处理器进行调用 doDispatch**

```java
	private void doReput() {
        	//如果reputFromOffset偏移量比commitLog最小偏移量还小，就从最小偏移量开始重投
            if (this.reputFromOffset < DefaultMessageStore.this.commitLog.getMinOffset()) {
                this.reputFromOffset = DefaultMessageStore.this.commitLog.getMinOffset();
            }
        
        	//偏移量没超过commitLog最大就一直循环重投
            for (boolean doNext = true; this.isCommitLogAvailable() && doNext; ) {

				//根据偏移量获取MappedFile以及缓冲池
                SelectMappedBufferResult result = DefaultMessageStore.this.commitLog.getData(reputFromOffset);
                if (result != null) {
                    try {
                        this.reputFromOffset = result.getStartOffset();
						//每次获取一条消息进行调用
                        for (int readSize = 0; readSize < result.getSize() && doNext; ) {
                            //检查并解析每条消息
                            DispatchRequest dispatchRequest =
                                DefaultMessageStore.this.commitLog.checkMessageAndReturnSize(result.getByteBuffer(), false, false);
                            int size = dispatchRequest.getBufferSize() == -1 ? dispatchRequest.getMsgSize() : dispatchRequest.getBufferSize();

                            if (dispatchRequest.isSuccess()) {
                                if (size > 0) {
                                    //成功进行调用
                                    DefaultMessageStore.this.doDispatch(dispatchRequest);
                                    
                                    //如果不止从节点且 开启长轮询 且 消息到达监听器不为空 会调用消息到达监听器 用于消费的长轮询（下篇文章再说消息消费）
									if (BrokerRole.SLAVE != DefaultMessageStore.this.getMessageStoreConfig().getBrokerRole()
                                            && DefaultMessageStore.this.brokerConfig.isLongPollingEnable()
                                            && DefaultMessageStore.this.messageArrivingListener != null) {
                                        //调用消息到达监听器
                                        DefaultMessageStore.this.messageArrivingListener.arriving(dispatchRequest.getTopic(),
                                            dispatchRequest.getQueueId(), dispatchRequest.getConsumeQueueOffset() + 1,
                                            dispatchRequest.getTagsCode(), dispatchRequest.getStoreTimestamp(),
                                            dispatchRequest.getBitMap(), dispatchRequest.getPropertiesMap());
                                        notifyMessageArrive4MultiQueue(dispatchRequest);
                                    }
                                    //...
                                } else if (size == 0) {
                                    this.reputFromOffset = DefaultMessageStore.this.commitLog.rollNextFile(this.reputFromOffset);
                                    readSize = result.getSize();
                                }
                            } 
                    } finally {
                        result.release();
                    }
                } else {
                    doNext = false;
                }
            }
        }

```

`doDispatch`方法会遍历 `CommitLogDispatcher` 进行处理

```java
public void doDispatch(DispatchRequest req) {
    for (CommitLogDispatcher dispatcher : this.dispatcherList) {
        dispatcher.dispatch(req);
    }
}
```

###### CommitLogDispatcherBuildConsumeQueue 追加数据

CommitLogDispatcherBuildConsumeQueue 会调用`putMessagePositionInfo`

```java
public void putMessagePositionInfo(DispatchRequest dispatchRequest) {
    ConsumeQueue cq = this.findConsumeQueue(dispatchRequest.getTopic(), dispatchRequest.getQueueId());
    cq.putMessagePositionInfoWrapper(dispatchRequest, checkMultiDispatchQueue(dispatchRequest));
}
```

**先通过topic、队列id获取对应的ConsumerQueue**，然后调用`putMessagePositionInfo`

**先在ConsumerQueue的缓冲区上写ConsumerQueue记录（消息偏移量、消息大小、tag哈希），然后获取映射文件MappedFile，最后再往文件里追加数据**

```java
private boolean putMessagePositionInfo(final long offset, final int size, final long tagsCode,
    final long cqOffset) {

    //先在自己的缓冲区上写数据
    this.byteBufferIndex.flip();
    this.byteBufferIndex.limit(CQ_STORE_UNIT_SIZE);
    //8B 消息在CommitLog偏移量
    this.byteBufferIndex.putLong(offset);
    //4B 消息大小
    this.byteBufferIndex.putInt(size);
    //8B tag哈希
    this.byteBufferIndex.putLong(tagsCode);

    final long expectLogicOffset = cqOffset * CQ_STORE_UNIT_SIZE;

    //获取MappedFile
    MappedFile mappedFile = this.mappedFileQueue.getLastMappedFile(expectLogicOffset);
    if (mappedFile != null) {

        //...
        
        //往文件里追加消息
        return mappedFile.appendMessage(this.byteBufferIndex.array());
    }
    return false;
}
```

注意MappedFile是映射文件，虽然ConsumerQueue与CommitLog都是往MappedFile追加数据，但它们的映射的文件不同

其实流程是类似的，这里只追加了数据，剩下的刷盘还要其他组件异步去做

###### FlushConsumeQueueService 异步刷盘

**FlushConsumeQueueService 负责ConsumerQueue文件的异步刷盘**

根据配置`flushIntervalConsumeQueue`刷盘频率默认1000ms，也就是循环中的等待时间，等待后调用`doFlush`进行刷盘

流程与异步刷盘类型，也是**读取刷盘页数、频率、间隔等参数进行刷盘**

不同的是**ConsumerQueue文件要从Topic、队列ID目录一层一层遍历刷盘**

```java
private void doFlush(int retryTimes) {
    //刷盘页数 默认2页
    int flushConsumeQueueLeastPages = DefaultMessageStore.this.getMessageStoreConfig().getFlushConsumeQueueLeastPages();

    if (retryTimes == RETRY_TIMES_OVER) {
        flushConsumeQueueLeastPages = 0;
    }

    long logicsMsgTimestamp = 0;
	//两次刷盘间隔 默认60s
    int flushConsumeQueueThoroughInterval = DefaultMessageStore.this.getMessageStoreConfig().getFlushConsumeQueueThoroughInterval();
    long currentTimeMillis = System.currentTimeMillis();
    if (currentTimeMillis >= (this.lastFlushTimestamp + flushConsumeQueueThoroughInterval)) {
        //如果超过间隔时间 则尽量刷盘
        this.lastFlushTimestamp = currentTimeMillis;
        flushConsumeQueueLeastPages = 0;
        logicsMsgTimestamp = DefaultMessageStore.this.getStoreCheckpoint().getLogicsMsgTimestamp();
    }

    //Key为Topic  Value中的Key则是Topic下的队列ID，ConsumerQueue则是队列ID目录下的ConsumerQueue文件
    ConcurrentMap<String, ConcurrentMap<Integer, ConsumeQueue>> tables = DefaultMessageStore.this.consumeQueueTable;

    //遍历Topic
    for (ConcurrentMap<Integer, ConsumeQueue> maps : tables.values()) {
        //遍历队列
        for (ConsumeQueue cq : maps.values()) {
            //遍历队列下的ConsumerQueue文件
            boolean result = false;
            for (int i = 0; i < retryTimes && !result; i++) {
                //ConsumerQueue文件刷盘
                result = cq.flush(flushConsumeQueueLeastPages);
            }
        }
    }

    //...
}
```

ConsumerQueue刷盘优先对自己进行刷盘，如果开启扩展文件(consumerqueue\_ext)也会对其进行刷盘

```java
public boolean flush(final int flushLeastPages) {
    boolean result = this.mappedFileQueue.flush(flushLeastPages);
    if (isExtReadEnable()) {
        result = result & this.consumeQueueExt.flush(flushLeastPages);
    }

    return result;
}
```

`this.mappedFileQueue.flush`刷盘上文以及说过，就不再分析了

ConsumerQueue记录对应的消息被消费完就不需要进行存储，因此也会有清理文件的组件

清理文件的组件是MessageStore下的`CleanConsumeQueueService`，同时它也会去清理InfexFile文件（流程类似，这里就不多说了）

至此ConsumerQueue文件也进行了持久化，IndexFile文件追加数据的原理类似

##### 写IndexFile

写IndexFile也是由`ReputMessageService`重投的，由`CommitLogDispatcherBuildIndex`进行处理

**会先获取IndexFile**，最终调用`getAndCreateLastIndexFile`，主要流程为：

1.  **先从列表中获取最后一个IndexFile文件** （期间加读锁，因为列表是线程不安全的）
2.  **如果没获取到或者已满，则创建IndexFile后，加写锁放入列表再解锁**
3.  **如果创建IndexFile要启动刷盘的线程持久化上一个IndexFile**

```java
public IndexFile getAndCreateLastIndexFile() {
    IndexFile indexFile = null;
    IndexFile prevIndexFile = null;
    long lastUpdateEndPhyOffset = 0;
    long lastUpdateIndexTimestamp = 0;

    {
        //如果列表中有IndexFile则取最后一个IndexFile 期间加读锁
        this.readWriteLock.readLock().lock();
        if (!this.indexFileList.isEmpty()) {
            IndexFile tmp = this.indexFileList.get(this.indexFileList.size() - 1);
            if (!tmp.isWriteFull()) {
                indexFile = tmp;
            } else {
                //已满也要创建
                lastUpdateEndPhyOffset = tmp.getEndPhyOffset();
                lastUpdateIndexTimestamp = tmp.getEndTimestamp();
                prevIndexFile = tmp;
            }
        }

        this.readWriteLock.readLock().unlock();
    }

    if (indexFile == null) {
        try {
            //如果没获取到则创建IndexFile
            String fileName =
                this.storePath + File.separator
                    + UtilAll.timeMillisToHumanString(System.currentTimeMillis());
            indexFile =
                new IndexFile(fileName, this.hashSlotNum, this.indexNum, lastUpdateEndPhyOffset,
                    lastUpdateIndexTimestamp);
            
            //并把它放入列表中 期间加写锁
            this.readWriteLock.writeLock().lock();
            this.indexFileList.add(indexFile);
        } catch (Exception e) {
            log.error("getLastIndexFile exception ", e);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }

        if (indexFile != null) {
            //开启负责刷盘的线程 将上一个文件进行刷盘
            final IndexFile flushThisFile = prevIndexFile;
            Thread flushThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    IndexService.this.flush(flushThisFile);
                }
            }, "FlushIndexFileThread");

            flushThread.setDaemon(true);
            flushThread.start();
        }
    }

    return indexFile;
}
```

获取IndexFile后，就会调用putKey构建哈希索引

上篇文章说过，在默认消息发送的实现中，如果消息不是批量消息则会设置唯一标识

这里的**Key是通过topic、#、唯一标识拼接而成的**

```java
public boolean putKey(final String key, final long phyOffset, final long storeTimestamp) {
    if (this.indexHeader.getIndexCount() < this.indexNum) {
        int keyHash = indexKeyHashMethod(key);
        //哈希槽的位置
        int slotPos = keyHash % this.hashSlotNum;
        //哈希槽偏移量
        int absSlotPos = IndexHeader.INDEX_HEADER_SIZE + slotPos * hashSlotSize;

        try {
			//哈希槽的值 可以找到
            int slotValue = this.mappedByteBuffer.getInt(absSlotPos);
            if (slotValue <= invalidIndex || slotValue > this.indexHeader.getIndexCount()) {
                slotValue = invalidIndex;
            }

            //计算时间差 单位秒
            long timeDiff = storeTimestamp - this.indexHeader.getBeginTimestamp();
            timeDiff = timeDiff / 1000;
            if (this.indexHeader.getBeginTimestamp() <= 0) {
                timeDiff = 0;
            } else if (timeDiff > Integer.MAX_VALUE) {
                timeDiff = Integer.MAX_VALUE;
            } else if (timeDiff < 0) {
                timeDiff = 0;
            }

            //计算要写入索引项的偏移量位置
            int absIndexPos =
                IndexHeader.INDEX_HEADER_SIZE + this.hashSlotNum * hashSlotSize
                    + this.indexHeader.getIndexCount() * indexSize;

            //写入索引项记录 
            this.mappedByteBuffer.putInt(absIndexPos, keyHash);
            this.mappedByteBuffer.putLong(absIndexPos + 4, phyOffset);
            this.mappedByteBuffer.putInt(absIndexPos + 4 + 8, (int) timeDiff);
            this.mappedByteBuffer.putInt(absIndexPos + 4 + 8 + 4, slotValue);

            //覆盖哈希槽的值 下次再通过哈希槽找到的是这个新的索引项 头插法
            this.mappedByteBuffer.putInt(absSlotPos, this.indexHeader.getIndexCount());

            //...
        } catch (Exception e) {
            log.error("putKey exception, Key: " + key + " KeyHashCode: " + key.hashCode(), e);
        }
    } 
    //...
}
```

##### 小结

至此已经描述完从接收消息到消息持久化CommitLog的主流程，以及异步持久化ConsumerQueue与IndexFile文件的流程

为了达到高性能，**在这个持久化的过程中并不是同步的，也不是原子操作，这种持久化设计采用的是数据最终一致性**，一旦节点宕机，文件漏写就会导致数据不一致

为了解决这个问题，需要在broker启动时判断上次是否为异常宕机，持久化文件是否写齐，如果不齐需要有对应的数据恢复过程（本文就不分析这部分的源码了，后续文章再说）

因此还会涉及到消息的重投，从而引起重复消费，这也是RocketMQ在保证消息不丢的同时，不能保证消息不重复

最终将相关操作绘制成一张流程图，如下：

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240902/1725265755839583394.png)

### 总结

**Broker为了持久化消息会写很多文件，其中主要为CommitLog、ConsumerQueue、IndexFile文件**

**为了实现高性能的写入，写入文件通常都是使用mmap（内存映射）对应源码中的MappedFile**

**CommitLog为消息顺序写入的文件，通过顺序写的方式提高写入性能，文件名以起始偏移量命名，固定1G，消息被消费后会删除**

**ConsumerQueue文件为消息的逻辑文件，会以Topic、队列ID、偏移量进行多级目录分类，每个也是以起始偏移量命名，固定6MB可以存储30W条ConsumerQueue记录**

**ConsumerQueue记录固定为20B，8B记录消息在CommitLog上的偏移量，4B记录消息大小，8B记录tag哈希值，通过前两个字段可以快速在CommitLog中找到对应消息，tag哈希值用于消息过滤，要拉取指定tag消息时，如果tag哈希值不对则过滤**

**IndexFile索引文件，可以看成消息Key和消息在CommitLog上偏移量的哈希索引文件，key由topic和消息唯一标识组成，通过key的哈希值模上哈希槽数量得到对应的哈希槽，根据哈希槽找到对应索引项，索引项上存储消息偏移量，能够快速找到消息（如果冲突则根据指针寻找下一个索引项）**

**默认刷盘和主从复制的方式都是异步，性能好但可靠性不好，同步虽然性能差但可靠性较好**

**Broker会使用Netty接收请求，再通过各种Processor对各种请求进行处理，如果是发送消息的请求最终会使用MessageStore进行存储**

**MessageStore复制消息的存储，读、写、清理、管理各种消息持久化相关文件**

**MessageStore会调用CommitLog进行写消息，CommitLog则是通过它的MappedFile进行写数据，在此期间可能多个线程需要写同一个MappedFile，因此需要加锁**

**如果没有或需要扩容，就要创建MappedFile，CommitLog将创建MappedFile的操作异步交给AllocateMappedFileService初始化MappedFile**

**MappedFile写完数据，数据就被映射在内核缓冲区，但此时还没有刷入磁盘，写完数据，还需要提交刷盘请求和同步请求**

**如果使用的是异步刷盘，则唤醒FlushRealTimeService的线程，根据配置的频率、页数、间隔等参数进行异步刷盘**

**如果使用的是同步刷盘，则将请求放入GroupCommitService的写队列中（需要使用同步手段防止并发），后续如果没返回值会阻塞**

**GroupCommitService会每隔10ms循环消费读队列中的请求进行刷盘，消费前等待时会将读写队列转换（因为写队列可能多线程写，而读队列只有一个线程读）**

**同步请求会交给HAService进行处理，流程类型刷盘，只不过过程是网络通信**

**最后主流程会等待刷盘、同步请求执行完，同步的话会阻塞，如果期间超时则会返回对应的响应状态，标识消息持久化可能失败（CommitLog持久化流程结束）**

**消费被消费后则不需要再存储，MessageStore会使用CleanCommitLogService定时清理**

**写完主要的CommitLog文件后，MessageStore会异步使用ReputMessageService重投消息，根据它的偏移量获取CommitLog文件，然后封装每一条消息交给CommitLogDispatcher调度器执行**

**写ConsumerQueue文件由CommitLogDispatcherBuildConsumeQueue进行调度，通过消息的Topic和队列ID找到对应的ConsumerQueue，再往对应MappedFile上追加数据**

**对于ConsumerQueue文件的刷盘，MessageStore会使用FlushConsumeQueueService读取配置异步进行刷盘，由于ConsumerQueue根据Topic、队列ID进行多级分类，因此刷盘时也要多层遍历刷盘**

**同时MessageStore会使用CleanConsumeQueueService定时清理过期的ConsumerQueue、IndexFile文件**

**写IndexFile文件由CommitLogDispatcherBuildIndex调度，它会调用IndexFileService对IndexFile文件进行追加数据，期间创建新的IndexFile才对上一个文件进行异步刷盘**

**为了高性能，在消息持久化的过程中使用顺序写、MMap、最终一致性等特点，节点宕机可能导致文件数据未写入，因此启动时会检查文件是否写入成功，如果写入文件不齐全，还会涉及到恢复文件，消息重投，可能导致消息重复消费**

#### 最后（点赞、收藏、关注求求啦\~）

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜





## 末尾