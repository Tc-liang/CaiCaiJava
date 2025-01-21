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

**异步发送：发送完消息后立即响应，不需要阻塞等待，但需要设置监听器，当消息成功或失败时进行业务处理，可以在失败时进行重试等其他逻辑，通常用于追求响应时间的场景**

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

DefaultMessageStore下的 **`ReputMessageService`用于重投消息，它会根据CommitLog上的偏移量封装成请求，重投给其他`CommitLogDispatcher`进行后续处理**

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





## RocketMQ（四）：消费前如何拉取消息？（长轮询机制）

[上篇文章](https://juejin.cn/post/7412922870521184256)从Broker接收消息开始，到消息持久化到各种文件结束，分析完消息在Broker持久化的流程与原理

消费者消费消息前需要先从Broker进行获取消息，然后再进行消费

为了流程的完整性，本篇文章就先来分析下消费者是如何获取消息的，文章内容导图如下：

![](https://bbs-img.huaweicloud.com/blogs/img/20240924/1727140259433247675.png)

### 获取消息的方式

消费者并不是每次要消费一条数据就向Broker获取一条数据的，这样RPC的开销太大了，因此先从Broker获取一批数据到内存中，再进行消费

消费端获取消息通常有三种方式：**推送消息、拉取消息、长轮询（推拉结合）**

**推送消息：消息持久化到Broker后，Broker监听到有新消息，主动将消息推送到对应的消费者**

**Broker主动推送消息具有很好的实时性，但如果消费端没有流控，推送大量消息时会增加消费端压力，导致消息堆积、吞吐量、性能下降**

**拉取消息：消费端可以根据自身的能力主动向Broker拉取适量的消息，但不好预估拉取消息的频率，拉取太慢会导致实时性差，拉取太快可能导致压力大、消息堆积**

**长轮询：在拉取消息的基础上进行改进，如果在broker没拉取到消息，则会等待一段时间，直到消息到达或超时再触发拉取消息**

**长轮询相当于在拉取消息的同时，通过监听消息到达，增加推送的优点，将拉取、推送的优点结合，但长连接会更占资源，大量长连接会导致开销大**

RocketMQ中常用的消费者`DefaultMQPushConsumer`，虽然从名字看是“推送”的方式，但获取消息用的是长轮询的方式

这种特殊的拉取消息方式能到达实时推送的效果，并在消费者端做好流控（拉取消息达到阈值就延时拉取）以防压力过大

### 拉取消息原理

`DefaultMQPushConsumer`的内部实现`DefaultMQPushConsumerImpl`有一个MQ客户端实例`MQClientInstance`

它内部包含的`PullMessageService`组件，就是用于长轮询拉取消息的

**PullMessageService会使用DefaultMQPushConsumerImpl与Broker建立长连接并拉取消息，拉取的消息存放在本地内存队列（processQueue）中，方便后续给消费者消费**

其中涉及一些组件，先简单介绍，方便后续描述：

*   **ProcessQueue：从Broker拉取的消息存放在这个内存队列中**
    *   **底层使用有序的TreeMap来进行存储的，其中Key为偏移量、Value为存储的消息**

*   **PullRequest：拉取请求，拉取消息(以队列为)基本单位**

    *   **PullMessageService轮询时，每次取出PullRequest再进行后续流程**

    *   **存储消费者组、对应的MessageQueue（broker上的队列）、ProcessQueue（消费者内存队列）、拉取的偏移量等信息**

    *   **PullRequest（拉消息）、MessageQueue、ProcessQueue（存消息）一一对应**

*   **PullRequestQueue：PullRequest的队列**
    *   **由于消费者可能同时消费多条队列，每次拉取的基本单位又是以同个队列进行拉取，因此PullMessageService需要轮询取出PullRequest进行后续拉取流程**
    *   **拉取消息失败或下次拉取消息都会把PullRequest重新投入队列中，由后续PullMessageService轮询取出再进行拉取消息**

简化的流程为：

1.  从队列取出PullRequest，然后封装请求向Broker异步发送
2.  响应后通过回调将查到的消息放入其内存队列中，方便后续消费
3.  在此期间最终都会将PullRequest放回队列（失败可能延时放回），便于下次拉取该队列的消息

![](https://bbs-img.huaweicloud.com/blogs/img/20240904/1725412522846536156.png)

#### 发送拉取消息请求

PullMessageService启动时也会使用线程进行轮询，会**从pullRequestQueue取出PullRequest进行后续的拉取消息**

```java
public void run() {
	//...
    while (!this.isStopped()) {
		//取出PullRequest 没有则阻塞
        PullRequest pullRequest = this.pullRequestQueue.take();
        //拉取消息
        this.pullMessage(pullRequest);
    }
}
```

##### pullMessage 拉取消息前准备参数

`pullMessage`最终会调用`DefaultMQPushConsumerImpl.pullMessage`，代码虽然很多，但主要流程为**校验、获取参数、调用核心方法**

1.  **进行参数、状态、流控的校验，如果失败会调用`executePullRequestLater`后续延时50ms将拉取请求重新放回队列**中，也就是后续再进行该队列的消息拉取
2.  **如果是第一次执行，要获取消费进度的偏移量`computePullFromWhereWithException`，后续使用PullRequest上的nextOffset**（集群模式向Broker获取）
3.  **获取消费端相关信息（后续会封装成请求），创建回调，回调在RPC后调用**
4.  **执行拉取消息的核心方法 `pullKernelImpl`**

```java
public void pullMessage(final PullRequest pullRequest) {
    //获取内存队列
    final ProcessQueue processQueue = pullRequest.getProcessQueue();
	//内存队列设置最新的拉取时间
    pullRequest.getProcessQueue().setLastPullTimestamp(System.currentTimeMillis());

    //参数、状态校验、流控..

    //内存队列中的消息数量
    long cachedMessageCount = processQueue.getMsgCount().get();
    //内存队列中消息大小 MB
    long cachedMessageSizeInMiB = processQueue.getMsgSize().get() / (1024 * 1024);

    //如果数量太多 默认1000 说明当前已经消息堆积 需要进行流控 后续定时将拉取请求再放入队列中 后续再来拉取消息
    if (cachedMessageCount > this.defaultMQPushConsumer.getPullThresholdForQueue()) {
        this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
        return;
    }
	//消息太大 类似 同理 默认100MB
    if (cachedMessageSizeInMiB > this.defaultMQPushConsumer.getPullThresholdSizeForQueue()) {
        this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
        return;
    }

    //...
    
    //如果是第一次要去获取拉取消息的偏移量
    offset = this.rebalanceImpl.computePullFromWhereWithException(pullRequest.getMessageQueue());
    
	//获取当前Topic的订阅数据 
    final SubscriptionData subscriptionData = this.rebalanceImpl.getSubscriptionInner().get(pullRequest.getMessageQueue().getTopic());

    final long beginTimestamp = System.currentTimeMillis();
	
    //创建回调  这里的回调是从broker拉取消息后执行的回调 后面再分析，这里先省略代码
    PullCallback pullCallback = new PullCallback();

    //...
    try {
        //拉取消息核心实现
        this.pullAPIWrapper.pullKernelImpl(
            pullRequest.getMessageQueue(),
            subExpression,
            subscriptionData.getExpressionType(),
            subscriptionData.getSubVersion(),
            pullRequest.getNextOffset(),
            this.defaultMQPushConsumer.getPullBatchSize(),
            sysFlag,
            commitOffsetValue,
            BROKER_SUSPEND_MAX_TIME_MILLIS,
            CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND,
            CommunicationMode.ASYNC,
            pullCallback
        );
    } catch (Exception e) {
        log.error("pullKernelImpl exception", e);
        this.executePullRequestLater(pullRequest, pullTimeDelayMillsWhenException);
    }
}
```

##### computePullFromWhereWithException 获取拉取消息的偏移量

`computePullFromWhereWithException`方法由再平衡组件`RebalancePushImpl`调用

（再平衡是消费者间重新分配队列的机制，增加/减少队列、消费者都会触发再平衡机制，平均分配给消费者队列，PullRequest也是它分配的，细节后文再说）

这里的拉取消息偏移量又可以叫上一次消费的偏移量，因为拉取消息从上次消费的偏移量开始拉取

当消费者首次拉取消息时，需要查询拉取偏移量（即上一次消费的偏移量），广播模式下这个偏移量在消费者端记录，就可以从内存中获取

而**集群模式下，偏移量在broker记录，需要从broker获取，最终调用`fetchConsumeOffsetFromBroker`获取**

**`fetchConsumeOffsetFromBroker` 也是先去获取Broker信息，本地没有就从NameServer获取**

然后**通过客户端API的`queryConsumerOffset`发送获取消费偏移量的请求**

##### pullKernelImpl 拉取消息核心

在拉取消息核心方法中会去**获取Broker等信息、然后封装请求，再通过Netty调用**

```java
public PullResult pullKernelImpl(
        final MessageQueue mq,
        final String subExpression,
        final String expressionType,
        final long subVersion,
        final long offset,
        final int maxNums,
        final int sysFlag,
        final long commitOffset,
        final long brokerSuspendMaxTimeMillis,
        final long timeoutMillis,
        final CommunicationMode communicationMode,
        final PullCallback pullCallback
    ) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
    	//缓存中查broker信息
        FindBrokerResult findBrokerResult =
            this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(),
                this.recalculatePullFromWhichNode(mq), false);
    
    	//没查到 则RPC查NameServer 并加入缓存
        if (null == findBrokerResult) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            findBrokerResult =
                this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(),
                    this.recalculatePullFromWhichNode(mq), false);
        }

        if (findBrokerResult != null) {
            {
                //检查是否为空或tag过滤 不是则抛异常
                // check version
                if (!ExpressionType.isTagType(expressionType)
                    && findBrokerResult.getBrokerVersion() < MQVersion.Version.V4_1_0_SNAPSHOT.ordinal()) {
                    throw new MQClientException("The broker[" + mq.getBrokerName() + ", "
                        + findBrokerResult.getBrokerVersion() + "] does not upgrade to support for filter message by " + expressionType, null);
                }
            }
            int sysFlagInner = sysFlag;

            if (findBrokerResult.isSlave()) {
                sysFlagInner = PullSysFlag.clearCommitOffsetFlag(sysFlagInner);
            }

            PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
            requestHeader.setConsumerGroup(this.consumerGroup);
            requestHeader.setTopic(mq.getTopic());
            requestHeader.setQueueId(mq.getQueueId());
            requestHeader.setQueueOffset(offset);
            //最大拉取数量 默认 32
            requestHeader.setMaxMsgNums(maxNums);
            //系统标记
            requestHeader.setSysFlag(sysFlagInner);
            //已提交偏移量 用于消费进度
            requestHeader.setCommitOffset(commitOffset);
            //Broker最大支持时间 15S
            requestHeader.setSuspendTimeoutMillis(brokerSuspendMaxTimeMillis);
            //消息过滤表达式
            requestHeader.setSubscription(subExpression);
            //消息过滤版本 通常是当前时间ms
            requestHeader.setSubVersion(subVersion);
            //消息过滤类型 通常是TAG类型
            requestHeader.setExpressionType(expressionType);

            String brokerAddr = findBrokerResult.getBrokerAddr();
            if (PullSysFlag.hasClassFilterFlag(sysFlagInner)) {
                brokerAddr = computePullFromWhichFilterServer(mq.getTopic(), brokerAddr);
            }

            //netty RPC 请求 Broker
            PullResult pullResult = this.mQClientFactory.getMQClientAPIImpl().pullMessage(
                brokerAddr,
                requestHeader,
                timeoutMillis,
                communicationMode,
                pullCallback);

            return pullResult;
        }
		
    	//NameServer都没查到broker 抛出异常
        throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }
```

至此，发送拉取消息的请求的流程已经结束，其实整体流程与生产者发送消息流程也类似，比如参数校验、获取broker信息、封装请求、RPC调用、处理结果等

#### 回调处理结果

由于是异步拉取消息，收到结果后会执行回调，结果有四种情况：

1.  FOUND：找到消息
2.  NO\_NEW\_MSG：没有最新消息
3.  NO\_MATCHED\_MSG：消息不匹配
4.  OFFSET\_ILLEGAL：非法偏移量，可能太大或太小

回调代码也比较多，这里就不进行展示，直接总结：

1.  **无论回调成功，还是失败，最终都会将该PullRequest放回队列中，方便后续继续拉取消息**
2.  成功的情况下通常会**更新下次拉取消息的偏移量（PullRequest的nextOffset）、将消息放入内存队列（processQueue）、提交消费请求（异步消费）**

至此消费端拉取消息的流程已经结束，需要注意几个关键流程节点：

1. **PullMessageService轮询获取PullRequest进行拉取消息**

2. **拉取消息前需要收集各种消费端数据，如果是集群模式下，首次调用还需要向Broker获取拉取消息的偏移量**

3. **封装拉取消息请求、回调后向Broker拉取消息，成功后回调会将消息存入PullRequest对应的ProcessQueue，同时将PullRequest返回队列，还会提交消费请求后续进行异步消费**

   注意将消息存入内存队列ProcessQueue还会发送消费请求`DefaultMQPushConsumerImpl.this.consumeMessageService.submitConsumeRequest`，用于后续异步消费消息，这里图中暂时未画出，后文再描述消费过程

![](https://bbs-img.huaweicloud.com/blogs/img/20240904/1725430051270912970.png)

#### Broker处理查询消费偏移量请求

接下来再来看看Broker是如何获取消息并放回的

[上篇文章曾分析过：Broker服务端的Netty是如何接收请求的](https://juejin.cn/post/7412922870521184256#heading-7)，最终会让各种各样的Processor进行请求的处理

Broker由ConsumerManageProcessor来进行读写消费偏移量（写偏移量的原理留在分析消费流程的文章中说明）

**ConsumerManageProcessor会使用ConsumerOffsetManager的 `queryOffset` 获取消费偏移量**

**ConsumerOffsetManager中使用双层Map的offsetTable来存储消费偏移量**

```java
ConcurrentMap<String/* topic@group */, ConcurrentMap<Integer /*queueId*/, Long/*offset*/>> offsetTable = new ConcurrentHashMap<String, ConcurrentMap<Integer, Long>>(512);
```

**offsetTable的第一层Key为`topic@group`，用topic与消费组确定，第二层Key为队列ID**

**通过Topic、GroupName、队列ID等信息可以快速获取消费偏移量，如果没记录消费偏移量则使用该队列上的最小偏移量**（从头开始）

```java
private RemotingCommand queryConsumerOffset(ChannelHandlerContext ctx, RemotingCommand request)
    throws RemotingCommandException {
    //构建响应
    final RemotingCommand response =
        RemotingCommand.createResponseCommand(QueryConsumerOffsetResponseHeader.class);
    final QueryConsumerOffsetResponseHeader responseHeader =
        (QueryConsumerOffsetResponseHeader) response.readCustomHeader();
    //解析请求
    final QueryConsumerOffsetRequestHeader requestHeader =
        (QueryConsumerOffsetRequestHeader) request
            .decodeCommandCustomHeader(QueryConsumerOffsetRequestHeader.class);

    //通过消费组名称、topic、队列id获取消费偏移量
    long offset =
        this.brokerController.getConsumerOffsetManager().queryOffset(
            requestHeader.getConsumerGroup(), requestHeader.getTopic(), requestHeader.getQueueId());

    if (offset >= 0) {
        responseHeader.setOffset(offset);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
    } else {
        //没有消费偏移量记录，则用队列上最小的偏移量
        long minOffset =
            this.brokerController.getMessageStore().getMinOffsetInQueue(requestHeader.getTopic(),
                requestHeader.getQueueId());
        if (minOffset <= 0
            && !this.brokerController.getMessageStore().checkInDiskByConsumeOffset(
            requestHeader.getTopic(), requestHeader.getQueueId(), 0)) {
            responseHeader.setOffset(0L);
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);
        } else {
            response.setCode(ResponseCode.QUERY_NOT_FOUND);
            response.setRemark("Not found, V3_0_6_SNAPSHOT maybe this group consumer boot first");
        }
    }

    return response;
}
```

Broker加入查询消费偏移量后的流程如下：

 ![](https://bbs-img.huaweicloud.com/blogs/img/20240904/1725433444161674402.png)

#### Broker处理拉取消息请求

处理拉取消息请求的是PullMessageProcessor，它会调用`processRequest`处理请求

`processRequest`中的代码非常多，主要会**封装响应、处理前的一些校验、根据请求信息查询消息，最后根据响应状态分别做处理**

其实与写业务代码非常相同，这里它的核心方法是**使用MessageStore进行查询消息**

```java
final GetMessageResult getMessageResult =
        this.brokerController.getMessageStore().getMessage(requestHeader.getConsumerGroup(), requestHeader.getTopic(),requestHeader.getQueueId(), requestHeader.getQueueOffset(), requestHeader.getMaxMsgNums(), messageFilter)
```

（上篇文章说过持久化时写数据用的是MessageStore，现在读数据当然也是使用MessageStore）

在这个方法中做校验的代码也很多，主要会**使用ConsumerQueue记录过滤消息并快速找到CommitLog上的消息**

简化流程为：

1. **根据Topic、队列ID获取对应的ConsumerQueue**

   **`ConsumeQueue consumeQueue = findConsumeQueue(topic, queueId);`**

   （通过ConsumerQueue逻辑文件可以快速找到CommitLog上的消息）

2. **根据拉取偏移量获取ConsumerQueue文件（缓冲区），方便后续读数据**

   **`SelectMappedBufferResult bufferConsumeQueue = consumeQueue.getIndexBuffer(offset);`**

   （消费端的队列偏移量就是ConsumerQueue上的偏移量，每次拉取完会把下次拉取的偏移量返回进行更新）

3. **计算本次读取的ConsumerQueue最大偏移量，然后开始循环读取ConsumerQueue记录并进行后续处理**

   （由每次最大拉取消息数量maxMsgNums决定，每条ConsumerQueue记录为20B）

   **`int maxFilterMessageCount = Math.max(16000, maxMsgNums * ConsumeQueue.CQ_STORE_UNIT_SIZE);`**

4. **查询消息前，使用ConsumerQueue记录的tag哈希值进行消息过滤**

   **`messageFilter.isMatchedByConsumeQueue(isTagsCodeLegal ? tagsCode : null, extRet ? cqExtUnit : null)`**

5. **根据ConsumerQueue记录上存在的偏移量和消息大小，找到CommitLog上的消息（缓冲区）**

   **`SelectMappedBufferResult selectResult = this.commitLog.getMessage(offsetPy, sizePy);`**

6. **查询后二次消息过滤**

   **`messageFilter.isMatchedByCommitLog(selectResult.getByteBuffer().slice(), null)`**

7. **最后把本次找到的消息加入结果，并更新偏移量**，继续后续的循环

   **`getResult.addMessage(selectResult);`**

最后将结果响应写回给消费者，后续消费者回调会将消息放入processqueue内存队列，等待后续消费者进行消费

![](https://bbs-img.huaweicloud.com/blogs/img/20240904/1725434937129602910.png)

至此有消息拉取的流程结束



#### Broker长轮询

上文中还说过如果只是通过消费端轮询拉取的方式，可能会导致实时性不好，拉取频率也会不好控制

为了优化这些缺陷，在没消息拉取的情况下会**使用长轮询，每次等待5s再判断是否唤醒，如果超时或者监听到队列中有新的消息则会唤醒，并再次执行PullMessageProcessor拉取消息的流程，然后写回客户端**

**由于消费者客户端发送拉取消息的请求是异步的，因此在Broker上等待时并不会阻塞消费者拉取其他队列**

消费者在发送拉取消息请求时，有两个与长轮询相关的参数：

**BROKER\_SUSPEND\_MAX\_TIME\_MILLIS：Broker支持的最大超时时间，默认15000ms = 15S**

**CONSUMER\_TIMEOUT\_MILLIS\_WHEN\_SUSPEND：消费者网络请求最大超时时间，默认30000ms = 30s**

Broker中负责长轮询的组件是 PullRequestHoldService

当PullMessageProcess未找到消息时，**允许暂停的情况下会将参数封装成`PullRequest`放入PullRequestHoldService暂停请求**（因为此时没消息）

```java
case ResponseCode.PULL_NOT_FOUND:
	//允许
    if (brokerAllowSuspend && hasSuspendFlag) {
        //broker最大支持超时时间 默认15000ms
        long pollingTimeMills = suspendTimeoutMillisLong;
        //关闭长轮询的情况下 设置超时时间为 1000ms
        if (!this.brokerController.getBrokerConfig().isLongPollingEnable()) {
            pollingTimeMills = this.brokerController.getBrokerConfig().getShortPollingTimeMills();
        }

        //构建PullRequest
        String topic = requestHeader.getTopic();
        long offset = requestHeader.getQueueOffset();
        int queueId = requestHeader.getQueueId();
        PullRequest pullRequest = new PullRequest(request, channel, pollingTimeMills,
                this.brokerController.getMessageStore().now(), offset, subscriptionData, messageFilter);
        //由PullRequestHoldService存储
        this.brokerController.getPullRequestHoldService().suspendPullRequest(topic, queueId, pullRequest);
        //响应置空 暂时不会写回响应 因此达到长轮询的效果
        response = null;
        break;
    }
```

**PullRequestHoldService是维护长轮询的组件，当未找到消息暂停请求时，将请求进行存储**

```java
public void suspendPullRequest(final String topic, final int queueId, final PullRequest pullRequest) {
    //key:topic@队列id
    String key = this.buildKey(topic, queueId);
    //获取队列上暂停拉取请求的列表
    ManyPullRequest mpr = this.pullRequestTable.get(key);
    if (null == mpr) {
        //没有就创建
        mpr = new ManyPullRequest();
        ManyPullRequest prev = this.pullRequestTable.putIfAbsent(key, mpr);
        if (prev != null) {
            mpr = prev;
        }
    }
	//加入
    mpr.addPullRequest(pullRequest);
}
```

思考：相同Topic下同一个队列一般只能由一个消费者进行消费，这里为什么要用列表存储？

（可能是因为消费者可能再平衡导致对消费的队列进行改变，或因为断线重连导致多次请求）

**PullRequestHoldService会定时进行检测，如果长轮询就是5s，短轮询就是1秒**

```java
public void run() {
    while (!this.isStopped()) {
        try {
            if (this.brokerController.getBrokerConfig().isLongPollingEnable()) {
              //长轮询 5s
              this.waitForRunning(5 * 1000);
            } else {
              //1s
              this.waitForRunning(this.brokerController.getBrokerConfig().getShortPollingTimeMills());
            }
            //检查暂停请求
            this.checkHoldRequest();
        } catch (Throwable e) {
        }
    }
}
```

**检查暂停请求的流程就是遍历所有topic@队列下的拉取请求，判断是否有新消息到来，或者请求是否已超时**

```java
protected void checkHoldRequest() {
    //遍历 key：topic@队列id
    for (String key : this.pullRequestTable.keySet()) {
        String[] kArray = key.split(TOPIC_QUEUEID_SEPARATOR);
        if (2 == kArray.length) {
            String topic = kArray[0];
            int queueId = Integer.parseInt(kArray[1]);
            //根据topic、队列id获取队列当前最大偏移量 （通过比较暂停前记录的拉取偏移量可以知道是否有消息来了）
            final long offset = this.brokerController.getMessageStore().getMaxOffsetInQueue(topic, queueId);
            try {
                //检查 消息是否到达 或 请求是否超时
                this.notifyMessageArriving(topic, queueId, offset);
            } catch (Throwable e) {
                log.error("check hold request failed. topic={}, queueId={}", topic, queueId, e);
            }
        }
    }
}
```

在通知消息到达的方法`notifyMessageArriving`中，主要检查消息是否到达和超时：

1.  **比较偏移量判断消息是否到达，如果到达则判断是否满足消息过滤**
2.  **同时也会检查请求是否超时（就是broker最大支持的超时时间 默认15s）**

**如果消息到达或请求超时都会进行唤醒并尝试拉取消息，否则会进行暂停**

```java
public void notifyMessageArriving(final String topic, final int queueId, final long maxOffset, final Long tagsCode,long msgStoreTime, byte[] filterBitMap, Map<String, String> properties) {
    String key = this.buildKey(topic, queueId);
    //根据key找到暂停拉取请求列表
    ManyPullRequest mpr = this.pullRequestTable.get(key);
    if (mpr != null) {
        //复制出来进行处理并清空mpr
        List<PullRequest> requestList = mpr.cloneListAndClear();
        if (requestList != null) {
            List<PullRequest> replayList = new ArrayList<PullRequest>();
			//遍历队列上每个暂停的拉取请求
            for (PullRequest request : requestList) {
                //队列上最大偏移量
                long newestOffset = maxOffset;
                if (newestOffset <= request.getPullFromThisOffset()) {
                    //如果队列上最大偏移量不大于拉取偏移量则更新偏移量 后续被重新加入暂停列表
                    newestOffset = this.brokerController.getMessageStore().getMaxOffsetInQueue(topic, queueId);
                }

                //如果大于 说明消息到达
                if (newestOffset > request.getPullFromThisOffset()) {
                    boolean match = request.getMessageFilter().isMatchedByConsumeQueue(tagsCode,
                        new ConsumeQueueExt.CqExtUnit(tagsCode, msgStoreTime, filterBitMap));
                    // match by bit map, need eval again when properties is not null.
                    if (match && properties != null) {
                        match = request.getMessageFilter().isMatchedByCommitLog(null, properties);
                    }

                    if (match) {
                        try {
                        //消息到达并且满足过滤的情况下，唤醒并拉取消息
                        this.brokerController.getPullMessageProcessor().executeRequestWhenWakeup(request.getClientChannel(),
                                request.getRequestCommand());
                        } catch (Throwable e) {
                            log.error("execute request when wakeup failed.", e);
                        }
                        continue;
                    }
                }

                //超时也会唤醒并拉取消息
                if (System.currentTimeMillis() >= (request.getSuspendTimestamp() + request.getTimeoutMillis())) {
                    try {
                        this.brokerController.getPullMessageProcessor().executeRequestWhenWakeup(request.getClientChannel(),
                            request.getRequestCommand());
                    } catch (Throwable e) {
                        log.error("execute request when wakeup failed.", e);
                    }
                    continue;
                }

               	//加入重试列表 后续一起加入暂停拉取请求列表
                replayList.add(request);
            }

            //其他情况重新加入暂停列表
            if (!replayList.isEmpty()) {
                mpr.addPullRequest(replayList);
            }
        }
    }
}
```

**executeRequestWhenWakeup会使用PullMessageProcessor尝试拉取消息，但这次拉取消息，如果没消息是不会允许被暂停的**

```java
public void executeRequestWhenWakeup(final Channel channel,
                                     final RemotingCommand request) throws RemotingCommandException {
    Runnable run = new Runnable() {
        @Override
        public void run() {
            try {
                //调用PullMessageProcessor 第三个参数为false，这次不允许暂停
                final RemotingCommand response = PullMessageProcessor.this.processRequest(channel, request, false);

                //写回响应
                if (response != null) {
                    response.setOpaque(request.getOpaque());
                    response.markResponseType();
                    try {
                        channel.writeAndFlush(response).addListener(new ChannelFutureListener() {
							//...
                        });
                    } catch (Throwable e) {
                    }
                }
            } catch (RemotingCommandException e1) {
            }
        }
    };
    //提交任务执行
    this.brokerController.getPullMessageExecutor().submit(new RequestTask(run, channel, request));
}
```

疑问：PullRequestHoldService长轮询下5S才检查一次请求，如果期间消息到达，那岂不是延迟太高了？

**为了解决这个实时性差的问题，引入消息到达监听器`MessageArrivingListener`，当消息到达时能够处理一些事情，比如进行通知的NotifyMessageArrivingListener**

它也是**通过调用`this.pullRequestHoldService.notifyMessageArriving`通知请求拉取消息**

那么监听器触发的时机在哪呢？看过[上篇文章](https://juejin.cn/post/7412922870521184256#heading-9)的同学就会知道，**在消息重投进行调度写ConsumerQueue、IndexFile等其他文件后，会触发这个监听器**（上篇文章也贴过此处源码）

```java
//成功进行调度写ConsumerQueue、IndexFile等其他文件
DefaultMessageStore.this.doDispatch(dispatchRequest);
//如果不止从节点且 开启长轮询 且 消息到达监听器不为空 会调用消息到达监听器 用于消费的长轮询
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
```

至此Broker长轮询机制的流程已经结束，小结一下流程：

1.  **未在Broker拉取到消息时会暂停请求**
2.  **由pullRequestHoldService定时检查或消息到达监听器进行处理**
3.  **如果消息到达并且满足匹配（不满足过滤条件）、请求超时，都会再次尝试进行拉取消息（这次的消息不会暂停）**
4.  **后续再写回响应**

![](https://bbs-img.huaweicloud.com/blogs/img/20240906/1725591772890701723.png)

### 总结

**消息中间件消费端获取消息的方式通常有推送、拉取、长轮询（推拉结合）三种**

**Broker主动推送消息有很好的实时性，但消费端未做流控可能会压力大，导致吞吐量、性能下降，消息积压**

**消费者主动拉取消息能根据自己的消费能力决定拉取数量，但无法预估拉取频率，太慢会导致实时性差**

**长轮询是特殊的拉取方式，在拉取的基础上，如果未拉取到消息会进行等待，超时或消息到达后再进行拉取，弥补拉取方式实时性差的缺点，但大量长连接一直等待资源开销大**

**PullMessageService组件用于消息拉取，每次拉取以队列为单位，会从队列轮询获取PullRequest进行消息拉取**

**发送拉取消息API前会收集消费端参数作为请求内容，如果是首次消费还要先向Broker获取消费偏移量，才知道后续要从哪里进行拉取**

**最后发送拉取消息请求，由于该请求是长连接，可能会一直阻塞不返回，为了不阻塞拉取其他队列消息，这里使用异步发送，通过回调处理响应**

**收到响应后会把本次PullRequest重新返回队列，如果拉取到消息，还要把消息放入PullRequest对应的ProcessorQueue内存队列中并提交消费请求，后续消费时通过该内存队列获取消息**

**Broker使用ConsumerManageProcessor处理查询/修改消费偏移量的请求，读写消费偏移量其实就是读写ConsumerOffsetManager组件维护的Map（根据topic、消费者组、队列id读写Map中的消费偏移量）**

**Broker使用PullMessageProcessor处理拉取消息的请求，会先通过topic、队列id获取ConsumerQueue，然后循环解析ConsumerQueue记录，通过记录进行消息过滤（比较tag哈希值），最后通过ConsumerQueue记录的偏移量和消息大小信息，查找CommitLog上的消息，加入结果集，最后写回响应**

**Broker处理长轮询的组件是PullRequestHoldService，当拉取消息请求找不到消息时，会暂停请求存在PullRequestHoldService中，等到PullRequestHoldService定时检测或消息到达监听器触发，去通知消息到达，如果消息到达并且匹配（不被消息过滤）或暂停请求超时都会触发拉取消息，但这次拉取消息不能再暂停请求，是否有响应都会写回**





#### 最后（点赞、收藏、关注求求啦\~）

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜









## RocketMQ（五）：揭秘高吞吐量并发消费原理

上篇文章已经描述过拉取消息的流程，消息被拉取到消费者内存后就会提交消费消息请求从而开始消费消息

消费消息分为两种方式：**并发消费、顺序消费**

1.  **并发消费采用多线程进行消费，能够大大提升消费吞吐量，但无法保证消费顺序**
2.  **顺序消费会通过加锁的方式进行有序消费，但吞吐量、性能不如并发**

本篇文章就来聊聊并发消费，揭秘RocketMQ高吞吐量并发消费的原理


思维导图如下：


![](https://bbs-img.huaweicloud.com/blogs/img/20241023/1729650541817873299.png)

>往期好文：


[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)



### 消费者消费流程

#### ConsumeMessageConcurrentlyService 消费消息

上篇文章说到，拉取完消息后会提交消费请求，便于后续进行异步消费

```java
DefaultMQPushConsumerImpl.this.consumeMessageService.submitConsumeRequest
```

`submitConsumeRequest` 方法有并发、顺序两种实现，先来查看并发实现

> ConsumeMessageConcurrentlyService.submitConsumeRequest

**并发的实现主要会根据每次批量消费的最大数量进行构建请求并提交，如果期间失败会延时5s再提交**

```java
public void submitConsumeRequest(
    final List<MessageExt> msgs,
    final ProcessQueue processQueue,
    final MessageQueue messageQueue,
    final boolean dispatchToConsume) {
    //每次批量消费最大数量 默认为1 （消息充足的情况下，默认每次拉取32条消息）
    final int consumeBatchSize = this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize();
    //如果拉取到的消息小于等于每次批量消费数量 则构建请求并提交
    if (msgs.size() <= consumeBatchSize) {
        ConsumeRequest consumeRequest = new ConsumeRequest(msgs, processQueue, messageQueue);
        try {
            this.consumeExecutor.submit(consumeRequest);
        } catch (RejectedExecutionException e) {
            //失败 延迟5s再提交请求
            this.submitConsumeRequestLater(consumeRequest);
        }
    } else {
        //如果拉取到的消息 大于 每次批量消费数量，则分批次构建请求并提交
        for (int total = 0; total < msgs.size(); ) {
            List<MessageExt> msgThis = new ArrayList<MessageExt>(consumeBatchSize);
            for (int i = 0; i < consumeBatchSize; i++, total++) {
                if (total < msgs.size()) {
                    msgThis.add(msgs.get(total));
                } else {
                    break;
                }
            }

            ConsumeRequest consumeRequest = new ConsumeRequest(msgThis, processQueue, messageQueue);
            try {
                this.consumeExecutor.submit(consumeRequest);
            } catch (RejectedExecutionException e) {
                for (; total < msgs.size(); total++) {
                    msgThis.add(msgs.get(total));
                }

                this.submitConsumeRequestLater(consumeRequest);
            }
        }
    }
}
```

并发、顺序实现中，构建的请求`ConsumeRequest` 并不是共享的，而是它们的内部类，虽然同名但是实现是不同的

提交后使用消费线程池执行，在执行任务的过程中，主要会**调用消费监听器进行消费消息 `consumeMessage`，然后通过成功/失败的情况进行处理结果`processConsumeResult`**

（在此期间还会封装上下文，执行消费前、后的钩子方法，记录消费耗时等操作）

```java
public void run() {
    //检查processQueue
    if (this.processQueue.isDropped()) {
        log.info("the message queue not be able to consume, because it's dropped. group={} {}", ConsumeMessageConcurrentlyService.this.consumerGroup, this.messageQueue);
        return;
    }

    //获取并发消息监听器 (我们设置消费者时实现的)
    MessageListenerConcurrently listener = ConsumeMessageConcurrentlyService.this.messageListener;
    //上下文 用于执行消费过程中的钩子方法
    ConsumeConcurrentlyContext context = new ConsumeConcurrentlyContext(messageQueue);
    //状态
    ConsumeConcurrentlyStatus status = null;
    //失败重试的情况下会更新topic
    defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, defaultMQPushConsumer.getConsumerGroup());

    //保留上下文信息 执行消费前的钩子方法时使用
    ConsumeMessageContext consumeMessageContext = null;
    if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
        consumeMessageContext = new ConsumeMessageContext();
        consumeMessageContext.setNamespace(defaultMQPushConsumer.getNamespace());
        consumeMessageContext.setConsumerGroup(defaultMQPushConsumer.getConsumerGroup());
        consumeMessageContext.setProps(new HashMap<String, String>());
        consumeMessageContext.setMq(messageQueue);
        consumeMessageContext.setMsgList(msgs);
        consumeMessageContext.setSuccess(false);
        ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.executeHookBefore(consumeMessageContext);
    }

    //计时
    long beginTimestamp = System.currentTimeMillis();
    //记录是否异常
    boolean hasException = false;
    ConsumeReturnType returnType = ConsumeReturnType.SUCCESS;
    try {
        //每个消息记录开始时间
        if (msgs != null && !msgs.isEmpty()) {
            for (MessageExt msg : msgs) {
                MessageAccessor.setConsumeStartTimeStamp(msg, String.valueOf(System.currentTimeMillis()));
            }
        }
        //消费消息
        status = listener.consumeMessage(Collections.unmodifiableList(msgs), context);
    } catch (Throwable e) {
        hasException = true;
    }
    //消费耗时
    long consumeRT = System.currentTimeMillis() - beginTimestamp;
    
    //计算返回类型
    if (null == status) {
        if (hasException) {
            returnType = ConsumeReturnType.EXCEPTION;
        } else {
            returnType = ConsumeReturnType.RETURNNULL;
        }
    } else if (consumeRT >= defaultMQPushConsumer.getConsumeTimeout() * 60 * 1000) {
        returnType = ConsumeReturnType.TIME_OUT;
    } else if (ConsumeConcurrentlyStatus.RECONSUME_LATER == status) {
        returnType = ConsumeReturnType.FAILED;
    } else if (ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status) {
        returnType = ConsumeReturnType.SUCCESS;
    }

    if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
        consumeMessageContext.getProps().put(MixAll.CONSUME_CONTEXT_TYPE, returnType.name());
    }

    if (null == status) {
        status = ConsumeConcurrentlyStatus.RECONSUME_LATER;
    }

    //消费后的钩子
    if (ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.hasHook()) {
        consumeMessageContext.setStatus(status.toString());
        consumeMessageContext.setSuccess(ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status);
        ConsumeMessageConcurrentlyService.this.defaultMQPushConsumerImpl.executeHookAfter(consumeMessageContext);
    }

    //增加消费时间 用于运营统计
    ConsumeMessageConcurrentlyService.this.getConsumerStatsManager()
        .incConsumeRT(ConsumeMessageConcurrentlyService.this.consumerGroup, messageQueue.getTopic(), consumeRT);

    if (!processQueue.isDropped()) {
        //处理消费结果
        ConsumeMessageConcurrentlyService.this.processConsumeResult(status, context, this);
    } else {
        log.warn("processQueue is dropped without process consume result. messageQueue={}, msgs={}", messageQueue, msgs);
    }
}
```

`processConsumeResult` 根据状态处理消费结果，分为两个状态：CONSUME_SUCCESS（成功）或RECONSUME_LATER（失败重试）和两种模式：广播、集群模式

无论成功还是失败都会统计对应的数据

**如果集群模式下失败，会调用 `sendMessageBack` 向Broker发送消息，将消息放入重试队列中，到期后进行重试；如果发送失败则延时5S重新进行消费**

**最终会移除ProcessorQueue中的消息并获取偏移量进行更新**

```java
public void processConsumeResult(
    final ConsumeConcurrentlyStatus status,
    final ConsumeConcurrentlyContext context,
    final ConsumeRequest consumeRequest
) {
    //默认int最大
    int ackIndex = context.getAckIndex();

    if (consumeRequest.getMsgs().isEmpty())
        return;

    switch (status) {
        case CONSUME_SUCCESS:
            //成功的情况下 ackIndex为本次消费数量-1
            if (ackIndex >= consumeRequest.getMsgs().size()) {
                ackIndex = consumeRequest.getMsgs().size() - 1;
            }
            int ok = ackIndex + 1;
            int failed = consumeRequest.getMsgs().size() - ok;
            //统计数据
            this.getConsumerStatsManager().incConsumeOKTPS(consumerGroup, consumeRequest.getMessageQueue().getTopic(), ok);
            this.getConsumerStatsManager().incConsumeFailedTPS(consumerGroup, consumeRequest.getMessageQueue().getTopic(), failed);
            break;
        case RECONSUME_LATER:
            //失败的情况下ackIndex为-1
            ackIndex = -1;
            //统计数据
            this.getConsumerStatsManager().incConsumeFailedTPS(consumerGroup, consumeRequest.getMessageQueue().getTopic(),
                consumeRequest.getMsgs().size());
            break;
        default:
            break;
    }

    switch (this.defaultMQPushConsumer.getMessageModel()) {
        case BROADCASTING:
            //如果能进入循环说明本次消费失败 广播模式 只打印日志 后续会调用本地的修改偏移量 相当于舍弃/删除 不处理
            for (int i = ackIndex + 1; i < consumeRequest.getMsgs().size(); i++) {
                MessageExt msg = consumeRequest.getMsgs().get(i);
                log.warn("BROADCASTING, the message consume failed, drop it, {}", msg.toString());
            }
            break;
        case CLUSTERING:
            List<MessageExt> msgBackFailed = new ArrayList<MessageExt>(consumeRequest.getMsgs().size());
            //如果能进入循环说明本次消费失败
            for (int i = ackIndex + 1; i < consumeRequest.getMsgs().size(); i++) {
                MessageExt msg = consumeRequest.getMsgs().get(i);
                //向broker发送消息 重试消息要放入对应的重试队列中
                boolean result = this.sendMessageBack(msg, context);
                if (!result) {
                    //发送失败记录
                    msg.setReconsumeTimes(msg.getReconsumeTimes() + 1);
                    msgBackFailed.add(msg);
                }
            }
            
			//发送失败延时消费
            if (!msgBackFailed.isEmpty()) {
                consumeRequest.getMsgs().removeAll(msgBackFailed);
                this.submitConsumeRequestLater(msgBackFailed, consumeRequest.getProcessQueue(), consumeRequest.getMessageQueue());
            }
            break;
        default:
            break;
    }

    //删除本次消费消息，获取偏移量
    long offset = consumeRequest.getProcessQueue().removeMessage(consumeRequest.getMsgs());
    if (offset >= 0 && !consumeRequest.getProcessQueue().isDropped()) {
        //更新偏移量
        this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), offset, true);
    }
}
```

**广播模式使用 `LocalFileOffsetStore` 更新偏移量，集群模式使用 `RemoteBrokerOffsetStore` 更新偏移量**

它们都是在内存更新偏移量，但**`RemoteBrokerOffsetStore`会定期向Broker进行更新消费偏移量**

```java
public void updateOffset(MessageQueue mq, long offset, boolean increaseOnly) {
    if (mq != null) {
        //mq旧偏移量
        AtomicLong offsetOld = this.offsetTable.get(mq);
        if (null == offsetOld) {
            offsetOld = this.offsetTable.putIfAbsent(mq, new AtomicLong(offset));
        }

        if (null != offsetOld) {
            //递增 CAS替换
            if (increaseOnly) {
                MixAll.compareAndIncreaseOnly(offsetOld, offset);
            } else {
                offsetOld.set(offset);
            }
        }
    }
}
```

顺序模式消费消息流程类似但加锁较为复杂，后文再详细说明

总结一下并发消费流程：

1. **拉取到消息后，回调中还会提交消费请求submitConsumerRequest**
2. **根据最大消费消息数量，将本次拉取的消息进行分批次构建请求ConsumerRequest并提交到线程池执行**
3. **执行ConsumerRequest任务主要调用消息监听器进行消费消息**（这里的逻辑是我们实现如何消费消息的，并返回状态），**并通过返回的状态处理消费结果**
4. **集群模式下消费失败会向Broker发送重试请求，如果发送失败会延时再次提交消费请求进行重新消费**
5. **如果消费成功，从ProcessorQueue中移除消息并更新内存中Broker的消费偏移量**（此时还没有向Broker提交更新消费偏移量的请求）

![](https://bbs-img.huaweicloud.com/blogs/img/20240910/1725960469899709352.png)



#### 定时更新消费偏移量

并发消费消息只是修改内存中Broker的消费偏移量

真正更新消费偏移量的是**MQClientInstance启动时的定时任务每10s调用`persistAllConsumerOffset`向Broker更新当前节点所有消费者的消费偏移量**

```java
this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

    @Override
    public void run() {
        try {
            MQClientInstance.this.persistAllConsumerOffset();
        } catch (Exception e) {
            log.error("ScheduledTask persistAllConsumerOffset exception", e);
        }
    }
}, 1000 * 10, this.clientConfig.getPersistConsumerOffsetInterval(), TimeUnit.MILLISECONDS);
```

`persistAllConsumerOffset`会遍历消费者进行持久化消费偏移量

```java
private void persistAllConsumerOffset() {
    //遍历消费者
    Iterator<Entry<String, MQConsumerInner>> it = this.consumerTable.entrySet().iterator();
    while (it.hasNext()) {
        Entry<String, MQConsumerInner> entry = it.next();
        MQConsumerInner impl = entry.getValue();
        //持久化消费偏移量
        impl.persistConsumerOffset();
    }
}
```



> DefaultMQPushConsumerImpl.persistConsumerOffset

**`persistConsumerOffset`会根据再平衡组件`RebalanceImpl`获取当前消费者负责消费的队列，再调用`this.offsetStore.persistAll`进行后续持久化**

（再平衡组件通过负载算法决定消费者负责消费哪些队列，后续文章再讲解再平衡机制）

```java
public void persistConsumerOffset() {
    try {
        this.makeSureStateOK();
        Set<MessageQueue> mqs = new HashSet<MessageQueue>();
        //获取负责消费的队列
        Set<MessageQueue> allocateMq = this.rebalanceImpl.getProcessQueueTable().keySet();
        //不破坏原容器
        mqs.addAll(allocateMq);
        this.offsetStore.persistAll(mqs);
    } catch (Exception e) {
        log.error("group: " + this.defaultMQPushConsumer.getConsumerGroup() + " persistConsumerOffset exception", e);
    }
}
```



> RemoteBrokerOffsetStore.persistAll

**遍历每个队列调用`updateConsumeOffsetToBroker`向Broker更新消费偏移量，如果有队列未使用则从offsetTable中移除**

```java
public void persistAll(Set<MessageQueue> mqs) {
    if (null == mqs || mqs.isEmpty())
        return;

    final HashSet<MessageQueue> unusedMQ = new HashSet<MessageQueue>();
	
    //遍历消费偏移量表 Key为队列 Value为偏移量
    for (Map.Entry<MessageQueue, AtomicLong> entry : this.offsetTable.entrySet()) {
        MessageQueue mq = entry.getKey();
        AtomicLong offset = entry.getValue();
        if (offset != null) {
            if (mqs.contains(mq)) {
                try {
                    //向Broker进行更新消费偏移量
                    this.updateConsumeOffsetToBroker(mq, offset.get());
                    log.info("[persistAll] Group: {} ClientId: {} updateConsumeOffsetToBroker {} {}",
                        this.groupName,
                        this.mQClientFactory.getClientId(),
                        mq,
                        offset.get());
                } catch (Exception e) {
                    log.error("updateConsumeOffsetToBroker exception, " + mq.toString(), e);
                }
            } else {
                //记录未使用的队列
                unusedMQ.add(mq);
            }
        }
    }

    //未使用的队列进行移除
    if (!unusedMQ.isEmpty()) {
        for (MessageQueue mq : unusedMQ) {
            this.offsetTable.remove(mq);
            log.info("remove unused mq, {}, {}", mq, this.groupName);
        }
    }
}
```



> RemoteBrokerOffsetStore.updateConsumeOffsetToBroker

定时任务调用的，第三个参数isOneway默认为true，这就说明是异步发送请求，无需关心是否发送/响应成功

**在更新Broker前还需要获取Broker信息（本地内存未获取到就从NameServer获取，再存入本地内存）、封装请求，再通过RPC请求Broker**

```java
public void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException,
    MQBrokerException, InterruptedException, MQClientException {
    //获取Broker信息    
    FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), MixAll.MASTER_ID, true);
    if (null == findBrokerResult) {
        this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
        findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), MixAll.MASTER_ID, false);
    }

    if (findBrokerResult != null) {
        //封装请求
        UpdateConsumerOffsetRequestHeader requestHeader = new UpdateConsumerOffsetRequestHeader();
        requestHeader.setTopic(mq.getTopic());
        requestHeader.setConsumerGroup(this.groupName);
        requestHeader.setQueueId(mq.getQueueId());
        requestHeader.setCommitOffset(offset);

        if (isOneway) {
            //异步
            this.mQClientFactory.getMQClientAPIImpl().updateConsumerOffsetOneway(
                findBrokerResult.getBrokerAddr(), requestHeader, 1000 * 5);
        } else {
            //同步阻塞
            this.mQClientFactory.getMQClientAPIImpl().updateConsumerOffset(
                findBrokerResult.getBrokerAddr(), requestHeader, 1000 * 5);
        }
    } else {
        throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }
}
```

总的来说，**定时更新就是遍历消费者的每个队列，向Broker提交异步更新每个队列消费偏移量的请求**

![定时向Broker更新消费偏移量](https://bbs-img.huaweicloud.com/blogs/img/20240910/1725961604864900807.png)

梳理完消费者消费消息的流程，再来看下Broker在该流程中需要处理的两个请求：消费失败的请求和消费成功更新消费偏移量的请求





### Broker处理流程

#### Broker处理更新消费偏移量请求

更新消费偏移量的请求码为`UPDATE_CONSUMER_OFFSET`，上篇文章在讲拉取消息时向Broker读取消费偏移量的请求码为`QUERY_CONSUMER_OFFSET`

处理读写消费偏移量请求的都是相同组件`ConsumerManageProcessor`

**读写消费偏移量实际上都是对Broker内存管理偏移量的`ConsumerOffsetManager`进行读写它的双层Map`offsetTable`，其中Key为`Topic@消费者组`Value则为队列ID与消费偏移量的映射**

```java
private void commitOffset(final String clientHost, final String key, final int queueId, final long offset) {
    //根据Key获取 队列和消费偏移量映射
    ConcurrentMap<Integer, Long> map = this.offsetTable.get(key);
    if (null == map) {
        //没有就初始化更新完放入
        map = new ConcurrentHashMap<Integer, Long>(32);
        map.put(queueId, offset);
        this.offsetTable.put(key, map);
    } else {
        //内存更新
        Long storeOffset = map.put(queueId, offset);
        if (storeOffset != null && offset < storeOffset) {
            log.warn("[NOTIFYME]update consumer offset less than store. clientHost={}, key={}, queueId={}, requestOffset={}, storeOffset={}", clientHost, key, queueId, offset, storeOffset);
        }
    }
}
```

Broker端的更新消费偏移量也是内存级别的操作，真正持久化也是定时刷盘的任务进行的（初始化延迟10S，后续5S定时刷盘）

```java
this.scheduledExecutorService.scheduleAtFixedRate(() -> {
    try {
        BrokerController.this.consumerOffsetManager.persist();
    } catch (Throwable e) {
        log.error("schedule persist consumerOffset error.", e);
    }
}, 1000 * 10, this.brokerConfig.getFlushConsumerOffsetInterval(), TimeUnit.MILLISECONDS);
```

异步刷盘的文件为`consumerOffset.json`，内容为JSON格式的双层Map`offsetTable`，第一层为topic@消费者组名，第二层Key为队列ID，VALUE为消费偏移量（如下JSON文件所示）

```java
{
	"offsetTable":{
		"%RETRY%warn_consumer_group@warn_consumer_group":{0:2
		},
		"%RETRY%order_consumer_group@order_consumer_group":{0:0
		},
		"TopicTest@please_rename_unique_group_name_5":{0:273,1:269,2:270,3:271,4:270,5:270,6:273,7:272
		},
		"TopicTest@order_consumer_group":{0:727914,1:727908,2:727913,3:727916,4:727910,5:727911,6:727918,7:727966
		},
		"TopicTest@warn_consumer_group":{0:727914,1:727908,2:727913,3:727916,4:727910,5:727911,6:727918,7:727966
		}
	}
}
```

**Broker使用ConsumerManagerProcessor负责处理消费相关请求，并使用管理消费偏移量的ConsumerOffsetManager根据topic、消费者组、队列id、消费偏移量等信息，对offsetTable进行更新消费偏移量，后续定时将offsetTable持久化为consumerOffset的JSON文件**

![Broker处理更新消费偏移量](https://bbs-img.huaweicloud.com/blogs/img/20240911/1726025404625349683.png)



#### Broker处理消费失败的请求

集群模式下，消费失败会向Broker发送请求码为`CONSUMER_SEND_MSG_BACK`的消息

处理该消息的Processor与处理生产者发送消息的相同，都是SendMessageProcessor

它会调用`asyncConsumerSendMsgBack`处理消费失败的请求

**处理时判断消息Topic是重试还是死信，然后再调用持久化消息`asyncPutMessage`的流程**

（最终投入消息的流程也是调用之前持久化消息说过的`asyncPutMessage`，只是投入前判断是放入哪个队列中）

不理解重试、死信等概念的同学可能不太懂这段源码，我们先来介绍下：

消息消费失败后会放入重试队列进行重试，其中无序消息重试消费的时间间隔会递增

当重试达到一定次数后认为“永远”无法消费，会将消息放入死信队列，放入死信队列可以让开发人员便于排查多次无法消费的原因

```java
private CompletableFuture<RemotingCommand> asyncConsumerSendMsgBack(ChannelHandlerContext ctx,
                                                                    RemotingCommand request) throws RemotingCommandException {
    //...

    
    //重试队列topic = %RETRY%+消费者组名
    String newTopic = MixAll.getRetryTopic(requestHeader.getGroup());
    int queueIdInt = ThreadLocalRandom.current().nextInt(99999999) % subscriptionGroupConfig.getRetryQueueNums();
    int topicSysFlag = 0;
    //...
    
    
    //根据便宜量查出原来存储的消息
    MessageExt msgExt = this.brokerController.getMessageStore().lookMessageByOffset(requestHeader.getOffset());
    
    //...
    
	//如果重试次数超过最大重试次数 默认16次  或 延迟等级为负数 则要放入死信队列
    if (msgExt.getReconsumeTimes() >= maxReconsumeTimes
        || delayLevel < 0) {
        //死信队列topic =  %DLQ% + 消费者组名
        newTopic = MixAll.getDLQTopic(requestHeader.getGroup());
        queueIdInt = ThreadLocalRandom.current().nextInt(99999999) % DLQ_NUMS_PER_GROUP;
        topicConfig = this.brokerController.getTopicConfigManager().createTopicInSendMessageBackMethod(newTopic,
                DLQ_NUMS_PER_GROUP,
                PermName.PERM_WRITE | PermName.PERM_READ, 0);
        //如果要放入死信 会设置延时等级为0
        msgExt.setDelayTimeLevel(0);
    } else {
        //重试的情况下 延时等级 = 3 + 消息重试次数
        if (0 == delayLevel) {
            delayLevel = 3 + msgExt.getReconsumeTimes();
        }
        //设置延时等级
        msgExt.setDelayTimeLevel(delayLevel);
    }

    //封装消息 异步存储
    MessageExtBrokerInner msgInner = new MessageExtBrokerInner();
    msgInner.setTopic(newTopic);
    msgInner.setBody(msgExt.getBody());
    msgInner.setFlag(msgExt.getFlag());
    //...

    CompletableFuture<PutMessageResult> putMessageResult = this.brokerController.getMessageStore().asyncPutMessage(msgInner);
    return putMessageResult.thenApply(r -> {
        //...
    });
}
```

重试Topic用`%RETRY%`与消费者组名进行拼接，用于消息的重试，并且只有一个队列用于存储需要重试的消息，那么它是如何做到不同时间间隔的消息到期后就进行重试的呢？

其实该流程中**不仅会用到重试队列、死信队列，还会用到延时队列**

当确认使用重试而不是死信队列时，会设置延时等级`msgExt.setDelayTimeLevel(delayLevel)`，使用死信时延时等级设置为0 `msgExt.setDelayTimeLevel(0)`

这个延时等级后续持久化消息前会进行判断，如果设置的延时等级大于0，说明需要使用延时队列

**每个级别的队列对应的延时时间不同，以此来实现等待一定的时间，还会将重试topic和队列id存入消息properties，延时到期后会将消息存入重试队列中，重试队列再被拉取消息进行重复消息**

```java
//消息设置过延时等级
if (msg.getDelayTimeLevel() > 0) {
    if (msg.getDelayTimeLevel() > this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel()) {
        msg.setDelayTimeLevel(this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel());
    }

    //topic换成延时topic
    topic = TopicValidator.RMQ_SYS_SCHEDULE_TOPIC;
    //根据延时等级获取对应的延时队列 第一次延时等级设置的是3，这个方法会减1，也就是第一次会被放入延时队列id为2
    int queueId = ScheduleMessageService.delayLevel2QueueId(msg.getDelayTimeLevel());

    // Backup real topic, queueId
    //将重试的topic、队列ID存入消息的properties，后续从延时队列出来重新存储时要使用
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_TOPIC, msg.getTopic());
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_QUEUE_ID, String.valueOf(msg.getQueueId()));
    msg.setPropertiesString(MessageDecoder.messageProperties2String(msg.getProperties()));

    //设置成延时topic和队列
    msg.setTopic(topic);
    msg.setQueueId(queueId);
}
```

第一次重试设置的延时等级为3，调用`delayLevel2QueueId`会将延时等级减1，也就是第一次进入延时的队列ID为2

延时队列ID范围为2-17，分别对应16个延时级别：

| 第几次重试 | 与上次重试的间隔时间 | 第几次重试 | 与上次重试的间隔时间 |
| ---------- | -------------------- | ---------- | -------------------- |
| 1          | 10秒                 | 9          | 7分钟                |
| 2          | 30秒                 | 10         | 8分钟                |
| 3          | 1分钟                | 11         | 9分钟                |
| 4          | 2分钟                | 12         | 10分钟               |
| 5          | 3分钟                | 13         | 20分钟               |
| 6          | 4分钟                | 14         | 30分钟               |
| 7          | 5分钟                | 15         | 1小时                |
| 8          | 6分钟                | 16         | 2小时                |

延时队列的原理与实现细节也很多，放在后文再进行解析

**Broker处理消费重试请求，实际上就是判断是否超过最大重试次数，如果超过放入死信队列、未超过放入重试队列并设置延时级别**

**而在CommitLog追加数据前，会判断是否设置延时级别，如果设置过要更改消息topic、queueid等信息，将其投入延时队列中**（这里只是写CommitLog，写延时队列的ConsumerQueue是异步重投的，前文已经说过）

**等到延时后消息从延时队列出来被投入重试队列中，后续继续被拉取消费**（延时队列的实现原理后文描述）

（图片原稿弄丢了，根据之前的图片贴上来补画的流程图，将就看~）

![Broker处理消费重试](https://bbs-img.huaweicloud.com/blogs/img/20240911/1726036230455720433.png)

你可能会有疑问，拉取消息需要通过PullRequest，而每个PullRequest对应一个队列，那么是谁把重试队列对应的PullRequest加入拉取消息的流程呢？

这也是再平衡机制进行处理的，后续的文章再来分析再平衡机制是如何为每个消费者分配队列的





### 总结

**提交消费请求后，会根据每次消费批处理最大消息数量进行分批次构建消费请求并提交到线程池执行任务**

**并发消费消息的特点是吞吐量大，使用线程池对拉取的消息进行消费，但是消费消息是无法预估执行顺序**

**消费消息时会使用消费者的消费监听器进行消费消息并获取返回状态，根据状态进行后续的处理（集群模式下）**

**如果状态为成功则删除ProcessQueue中的消息，并更新内存中记录Broker的消费偏移量，后续定时任务向Broker进行更新该消费者所有队列对应的消费偏移量**

**Broker更新队列的消费偏移量时，实际上也是在内存更新ConsumerOffsetManager的offsettable记录的消费偏移量，后续定时将其持久化到consumerOffset.json文件**

**如果状态为失败，则会向Broker发送消费重试的同步请求，如果请求超时或未发出去，则再次延时提交该消费请求，后续重新消费**

**Broker收到消费重试请求后，相当于又要进行持久化，只是期间会改变消息topic、队列等信息，根据重试次数判断是否超时最大重试次数，如果超时则将消息topic、队列等数据改为死信的，未超过则将消息的数据改为重试的，并设置延时级别**

**在CommitLog追加数据前，会判断消息是否设置过延时级别，如果设置过则又会将消息的topic、队列等数据改为延时的，并保存之前重试队列的数据，持久化消息后，异步写ComsumerQueue，相当于消息被投入延时队列中，等到延时时间结束后，消息会被投入重试队列**

**消费者的再平衡机制会将这个重试队列对应的PullRequest请求加入，后续再进行拉取消息并进行消费，以此达成消费重试机制**



#### 最后（点赞、收藏、关注求求啦\~）

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜





## RocketMQ（六）：Consumer Rebalanc原理解析（运行流程、触发时机、导致的问题）

之前的文章已经说过拉取消息和并发消费消息的原理，其中消费者会根据要负责的队列进行消息的拉取以及消费，而再平衡机制就是决定消费者要负责哪些队列的

RocketMQ设计上，一个队列只能被一个消费者进行消费，而消费者可以同时消费多个队列

**Consumer Rebalance 消费者再平衡机制用于将队列负载均衡到消费者**

可以把它理解成一个分配资源的动作，其中的资源就是队列，而把队列分配给哪些消费者负责就是再平衡机制决定的

当消费者上/下线，队列动态扩容/缩容，都会导致触发再平衡机制，导致重新分配资源

频繁触发可能会导致吞吐量降低、数据一致性等危害

本篇文章就来聊聊Consumer Rebalance（再/重平衡）机制的原理以及危害和预防方式，思维导图如下：

![思维导图](https://bbs-img.huaweicloud.com/blogs/img/20241105/1730770179185900288.png)



> 往期好文：

[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)

[RocketMQ（五）：揭秘高吞吐量并发消费原理](https://juejin.cn/post/7433066208825524243)



### 消费者再平衡 doRebalance 

负责再平衡的组件是RebalanceImpl，对应推拉消费模式，它也有推拉的实现：RebalancePushImpl、RebalanceLitePullImpl

还是以推的消费来查看源码，也就是DefaultMQPushConsumerImpl与RebalancePushImpl

> doRebalance

**`doRebalance`方法是再平衡的开始方法，会根据每个Topic进行再平衡**

（同一个Topic下的一个队列虽然只能由一个消费者负责，但是消费者可以负责多个Topic的队列）

```java
public void doRebalance(final boolean isOrder) {
    //获取当前消费者订阅信息
    Map<String, SubscriptionData> subTable = this.getSubscriptionInner();
    if (subTable != null) {
        //遍历每个Topic 
        for (final Map.Entry<String, SubscriptionData> entry : subTable.entrySet()) {
            final String topic = entry.getKey();
            try {
                //根据Topic进行再平衡
                this.rebalanceByTopic(topic, isOrder);
            } catch (Throwable e) {
                if (!topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    log.warn("rebalanceByTopic Exception", e);
                }
            }
        }
    }

    //清理不再需要的ProcessorQueue(没在该消费者订阅Topic)
    this.truncateMessageQueueNotMyTopic();
}
```

**进行完再平衡后，会调用`truncateMessageQueueNotMyTopic`清理不再需要的ProcessorQueue**

分析前必须要知道：**PullRequest、ProcessorQueue与MessageQueue一一对应，PullRequest用于拉取消息，拉取到消息将消息放入ProcessorQueue中后续进行消费，MessageQueue为操作它们时相当于操作（拉取消息、消费消息）哪个队列** （我愿称它们为“黄金铁三角”，但它们没有武魂融合技）





#### 根据Topic再平衡(核心方法) rebalanceByTopic 

根据Topic进行再平衡是再平衡的核心方法，分为广播、集群模式进行处理

**广播模式消费者要处理该Topic下所有的队列，而集群模式下会通过不同的策略来进行分配队列**

集群模式下再平衡的流程为：

1. **获取该Topic下所有队列`this.topicSubscribeInfoTable.get(topic)`**（内存获取，路由数据来源NameServer）
2. **向Broker获取该Topic下消费者组中的所有消费者（客户端ID） `this.mQClientFactory.findConsumerIdList(topic, consumerGroup)`**（由于消费者可能不在同一个节点上，但它们都会向Broker注册，因此要去Broker获取）
3. **所有队列和消费者客户端ID排序后，使用分配队列策略进行分配队列，默认平均哈希算法 `strategy.allocate`**（比如三个队列，三个消费者就平均每个消费者一个队列，如果是四个队列，那第一个消费者就多负责个队列）
4. **通过分配完的队列更新ProcessorQueue `this.updateProcessQueueTableInRebalance(topic, allocateResultSet, isOrder)`**（删除不需要再负责的ProcessorQueue以及新增需要负责的ProcessorQueue）
5. **如果更新ProcessorQueue，还要改变队列的流控配置以及向所有Broker进行心跳 `this.messageQueueChanged(topic, mqSet, allocateResultSet)`**

![再平衡集群模式主流程](https://bbs-img.huaweicloud.com/blogs/img/20240913/1726213489499634600.png)

> rebalanceByTopic

```java
private void rebalanceByTopic(final String topic, final boolean isOrder) {
    switch (messageModel) {
        //广播模式
        case BROADCASTING: {
            //获取当前消费者要负责的队列即topic下所有队列（这些队列就是当前消费者要负责的，广播下就是topic下所有队列，消费者都要负责）
            Set<MessageQueue> mqSet = this.topicSubscribeInfoTable.get(topic);
            if (mqSet != null) {
                //根据消费者要负责的队列更新ProcessQueue
                boolean changed = this.updateProcessQueueTableInRebalance(topic, mqSet, isOrder);
                if (changed) {
                    //更新ProcessQueue成功还要更改对应的消息队列
                    this.messageQueueChanged(topic, mqSet, mqSet);
                    log.info("messageQueueChanged {} {} {} {}",
                        consumerGroup,
                        topic,
                        mqSet,
                        mqSet);
                }
            } else {
                log.warn("doRebalance, {}, but the topic[{}] not exist.", consumerGroup, topic);
            }
            break;
        }
        case CLUSTERING: {
            //获取该Topic下所有队列
            Set<MessageQueue> mqSet = this.topicSubscribeInfoTable.get(topic);
            //向Broker查询该Topic下的这个消费者组的所有消费者客户端ID，方便后续给消费者分配队列
            List<String> cidAll = this.mQClientFactory.findConsumerIdList(topic, consumerGroup);

            if (mqSet != null && cidAll != null) {
                //所有队列 set->list
                List<MessageQueue> mqAll = new ArrayList<MessageQueue>();
                mqAll.addAll(mqSet);

                //所有队列、消费者客户端ID排序 方便分配
                Collections.sort(mqAll);
                Collections.sort(cidAll);

                //分配队列的策略 默认是AllocateMessageQueueAveragely平均哈希队列算法，在构建消费者DefaultMQPushConsumer时确定的
                AllocateMessageQueueStrategy strategy = this.allocateMessageQueueStrategy;

                //调用策略获取要分配给当前消费者的队列
                List<MessageQueue> allocateResult = null;
                try {
                    allocateResult = strategy.allocate(
                        this.consumerGroup,
                        this.mQClientFactory.getClientId(),
                        mqAll,
                        cidAll);
                } catch (Throwable e) {
                    log.error("AllocateMessageQueueStrategy.allocate Exception. allocateMessageQueueStrategyName={}", strategy.getName(),
                        e);
                    return;
                }

                //要分配的队列 list->set
                Set<MessageQueue> allocateResultSet = new HashSet<MessageQueue>();
                if (allocateResult != null) {
                    allocateResultSet.addAll(allocateResult);
                }

                //根据要分配的队列更新ProcessQueue
                boolean changed = this.updateProcessQueueTableInRebalance(topic, allocateResultSet, isOrder);
                if (changed) {
                    //更新ProcessQueue同时改变MessageQueue
                    this.messageQueueChanged(topic, mqSet, allocateResultSet);
                }
            }
            break;
        }
        default:
            break;
    }
}
```





#### 查询所有消费者ID findConsumerIdList 

在向Broker查当前Topic、消费者组下所有消费者ID时，也是老套路：

1. **先多级缓存查Broker地址信息，本地查不到就去NameServer**
2. **再通过API RPC请求Broker，其中请求码为`GET_CONSUMER_LIST_BY_GROUP`**

```java
public List<String> findConsumerIdList(final String topic, final String group) {
    //多级缓存查broker
    String brokerAddr = this.findBrokerAddrByTopic(topic);
    if (null == brokerAddr) {
        this.updateTopicRouteInfoFromNameServer(topic);
        brokerAddr = this.findBrokerAddrByTopic(topic);
    }

    if (null != brokerAddr) {
        try {
            //API请求
            return this.mQClientAPIImpl.getConsumerIdListByGroup(brokerAddr, group, clientConfig.getMqClientApiTimeout());
        } catch (Exception e) {
            log.warn("getConsumerIdListByGroup exception, " + brokerAddr + " " + group, e);
        }
    }

    return null;
}
```





#### 平均哈希算法分配队列 AllocateMessageQueueAveragely.allocate 

**平均哈希算法就是每个消费者先平均负责相同的队列，如果此时还有队列多出就按照消费者顺序依次多分配一个队列**

```java
@Override
public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll,
    List<String> cidAll) {
    //校验
    //...
    List<MessageQueue> result = new ArrayList<MessageQueue>();

	//当前消费者下标
    int index = cidAll.indexOf(currentCID);
    //模 相当于平均后多出来的队列
    int mod = mqAll.size() % cidAll.size();
    //得到平均值 如果mod > 0 并且 index < mod 说明平均后有队列多出来，index在mod前的消费者都要多分配一个队列
    int averageSize =
        mqAll.size() <= cidAll.size() ? 1 : (mod > 0 && index < mod ? mqAll.size() / cidAll.size()
            + 1 : mqAll.size() / cidAll.size());
    //开始的下标
    int startIndex = (mod > 0 && index < mod) ? index * averageSize : index * averageSize + mod;
    //加入队列的范围
    int range = Math.min(averageSize, mqAll.size() - startIndex);
    for (int i = 0; i < range; i++) {
        result.add(mqAll.get((startIndex + i) % mqAll.size()));
    }
    return result;
}
```

需要注意的是这里的cidAll是同组消费者ID列表，**如果多消费者组同时订阅相同的Topic（包括tag也相同），那么消费时会导致各消费者组都有消费者进行消息消费**



#### 根据分配队列更新ProcessQueue(updateProcessQueueTableInRebalance)

**先将不用负责的与拉取超时的ProcessQueue进行删除**（topic相同，但其对应的mq不在分配的mq中）

**如果有需要新增的ProcessQueue，会先删除本地存储broker该队列消费偏移量，再从broker请求该队列最新的消费偏移量**（之前拉取消息文章中已经分析过此流程）

**新增ProcessQueue时，如果之前没有维护过mq与ProcessQueue的对应关系，还要新增PullRequest，最后将PullRequest返回队列，便于后续拉取消息**

```java
private boolean updateProcessQueueTableInRebalance(final String topic, final Set<MessageQueue> mqSet,
    final boolean isOrder) {
    boolean changed = false;
	
    //当前消费者的processQueueTable迭代器
    Iterator<Entry<MessageQueue, ProcessQueue>> it = this.processQueueTable.entrySet().iterator();
    while (it.hasNext()) {
        Entry<MessageQueue, ProcessQueue> next = it.next();
        MessageQueue mq = next.getKey();
        ProcessQueue pq = next.getValue();

        if (mq.getTopic().equals(topic)) {
            //topic相同 但是mq不在分配的队列中就删除
            if (!mqSet.contains(mq)) {
                pq.setDropped(true);
                if (this.removeUnnecessaryMessageQueue(mq, pq)) {
                    it.remove();
                    changed = true;
                    log.info("doRebalance, {}, remove unnecessary mq, {}", consumerGroup, mq);
                }
            } else if (pq.isPullExpired()) {
                switch (this.consumeType()) {
                    case CONSUME_ACTIVELY:
                        break;
                    case CONSUME_PASSIVELY:
                        //拉取超时 默认120s 推的情况下删除
                        pq.setDropped(true);
                        if (this.removeUnnecessaryMessageQueue(mq, pq)) {
                            it.remove();
                            changed = true;
                            log.error("[BUG]doRebalance, {}, remove unnecessary mq, {}, because pull is pause, so try to fixed it",
                                consumerGroup, mq);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    List<PullRequest> pullRequestList = new ArrayList<PullRequest>();
    for (MessageQueue mq : mqSet) {
        //新增的队列要添加对应的ProcessQueue
        if (!this.processQueueTable.containsKey(mq)) {
			//先删除内存中存储的偏移量
            this.removeDirtyOffset(mq);
            
            ProcessQueue pq = new ProcessQueue();
            long nextOffset = -1L;
            try {
                //向Broker获取该队列的消费偏移量
                nextOffset = this.computePullFromWhereWithException(mq);
            } catch (Exception e) {
                log.info("doRebalance, {}, compute offset failed, {}", consumerGroup, mq);
                continue;
            }

            if (nextOffset >= 0) {
                //添加ProcessQueue 如果之前不存在还要添加PullRequest
                //（PullRequest拉取消息、ProcessQueue存储消息、MessageQueue一一对应）
                ProcessQueue pre = this.processQueueTable.putIfAbsent(mq, pq);
                if (pre != null) {
                    log.info("doRebalance, {}, mq already exists, {}", consumerGroup, mq);
                } else {
                    log.info("doRebalance, {}, add a new mq, {}", consumerGroup, mq);
                    PullRequest pullRequest = new PullRequest();
                    pullRequest.setConsumerGroup(consumerGroup);
                    pullRequest.setNextOffset(nextOffset);
                    pullRequest.setMessageQueue(mq);
                    pullRequest.setProcessQueue(pq);
                    pullRequestList.add(pullRequest);
                    changed = true;
                }
            } else {
                log.warn("doRebalance, {}, add new mq failed, {}", consumerGroup, mq);
            }
        }
    }

    //PullRequest放入队列，方便后续进行拉取消息
    this.dispatchPullRequest(pullRequestList);

    return changed;
}
```





#### 消息队列更改 messageQueueChanged

**如果删除过ProcessorQueue或新增过PullRequest，都要对队列的消息数量、大小流控进行更改，并加锁通知所有Broker**

**最终调用API请求码：HEART_BEAT向Broker进行心跳**

> RebalancePushImpl.messageQueueChanged

```java
public void messageQueueChanged(String topic, Set<MessageQueue> mqAll, Set<MessageQueue> mqDivided) {
    /**
     * When rebalance result changed, should update subscription's version to notify broker.
     * Fix: inconsistency subscription may lead to consumer miss messages.
     */
    SubscriptionData subscriptionData = this.subscriptionInner.get(topic);
    long newVersion = System.currentTimeMillis();
    log.info("{} Rebalance changed, also update version: {}, {}", topic, subscriptionData.getSubVersion(), newVersion);
    subscriptionData.setSubVersion(newVersion);

    //当前队列数量
    int currentQueueCount = this.processQueueTable.size();
    if (currentQueueCount != 0) {
        //更改消息数量流控：topic最多允许拉多少消息
        int pullThresholdForTopic = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdForTopic();
        if (pullThresholdForTopic != -1) {
            //每个队列允许存多少消息 
            int newVal = Math.max(1, pullThresholdForTopic / currentQueueCount);
            log.info("The pullThresholdForQueue is changed from {} to {}",
                this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdForQueue(), newVal);
            this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().setPullThresholdForQueue(newVal);
        }

        //更改消息大小流控 与数量类似
        int pullThresholdSizeForTopic = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdSizeForTopic();
        if (pullThresholdSizeForTopic != -1) {
            int newVal = Math.max(1, pullThresholdSizeForTopic / currentQueueCount);
            log.info("The pullThresholdSizeForQueue is changed from {} to {}",
                this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getPullThresholdSizeForQueue(), newVal);
            this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().setPullThresholdSizeForQueue(newVal);
        }
    }

    // notify broker
    //加锁通知所有broker
    this.getmQClientFactory().sendHeartbeatToAllBrokerWithLock();
}
```

**通知所有Broker时会将MQClientInstance记录的生产者、消费者都进行心跳**，因此要加锁避免重复





### Broker处理

再平衡过程中，Broker需要处理三种请求：

1. 查询队列的消费偏移量（以前文章分析过，这里不分析）
2. 查询消费者组下所有消费者
3. 消费者心跳

接下来依次分析Broker是如何处理的

#### 查询消费者组下所有消费者

请求码为`GET_CONSUMER_LIST_BY_GROUP`，处理的组件为ConsumerManageProcessor

（之前也分析过用它来读写消费偏移量，马上它所有处理的请求就都要分析完了）

```java
@Override
public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
    throws RemotingCommandException {
    switch (request.getCode()) {
        case RequestCode.GET_CONSUMER_LIST_BY_GROUP:
            return this.getConsumerListByGroup(ctx, request);
        case RequestCode.UPDATE_CONSUMER_OFFSET:
            return this.updateConsumerOffset(ctx, request);
        case RequestCode.QUERY_CONSUMER_OFFSET:
            return this.queryConsumerOffset(ctx, request);
        default:
            break;
    }
    return null;
}
```

**`getConsumerListByGroup`会使用ConsumerManager的consumerTable，根据消费者组获取ConsumerGroupInfo信息**

```java
ConcurrentMap<String/* Group */, ConsumerGroupInfo> consumerTable = new ConcurrentHashMap<String, ConsumerGroupInfo>(1024);

ConsumerGroupInfo consumerGroupInfo =
    this.brokerController.getConsumerManager().getConsumerGroupInfo(
        requestHeader.getConsumerGroup());
```

**ConsumerGroupInfo中消费者的信息被封装为ClientChannelInfo存储在channelInfoTable中，其中就可以提取到消费者的客户端ID**

```java
private final ConcurrentMap<Channel, ClientChannelInfo> channelInfoTable = new ConcurrentHashMap<Channel, ClientChannelInfo>(16);
```

![Broker查询消费者组内消费者ID](https://bbs-img.huaweicloud.com/blogs/img/20240913/1726215621847522338.png)

实现比较简单，其中的很多信息都是在心跳时更新的，接下来看下心跳的流程



#### 心跳

心跳的请求码为`HEART_BEAT`，由ClientManageProcessor负责处理，会调用`heartBeat`

**处理心跳的过程中会先处理消费者的心跳再处理生产者心跳**

```java
public RemotingCommand heartBeat(ChannelHandlerContext ctx, RemotingCommand request) {
    RemotingCommand response = RemotingCommand.createResponseCommand(null);
    //心跳数据 分为消费者和生产者的心跳
    HeartbeatData heartbeatData = HeartbeatData.decode(request.getBody(), HeartbeatData.class);
    //客户端channel 方便后续通信交互
    ClientChannelInfo clientChannelInfo = new ClientChannelInfo(
        ctx.channel(),
        heartbeatData.getClientID(),
        request.getLanguage(),
        request.getVersion()
    );

    //处理消费者心跳
    for (ConsumerData data : heartbeatData.getConsumerDataSet()) {
        //消费组订阅配置
        SubscriptionGroupConfig subscriptionGroupConfig =
            this.brokerController.getSubscriptionGroupManager().findSubscriptionGroupConfig(
                data.getGroupName());
        //消费组变更通知是否开启 后续变更通知组内消费者再平衡
        boolean isNotifyConsumerIdsChangedEnable = true;
        if (null != subscriptionGroupConfig) {
            isNotifyConsumerIdsChangedEnable = subscriptionGroupConfig.isNotifyConsumerIdsChangedEnable();
            int topicSysFlag = 0;
            if (data.isUnitMode()) {
                topicSysFlag = TopicSysFlag.buildSysFlag(false, true);
            }
            //创建重试队列的Topic
            String newTopic = MixAll.getRetryTopic(data.getGroupName());
            this.brokerController.getTopicConfigManager().createTopicInSendMessageBackMethod(
                newTopic,
                subscriptionGroupConfig.getRetryQueueNums(),
                PermName.PERM_WRITE | PermName.PERM_READ, topicSysFlag);
        }

        //注册消费者
        boolean changed = this.brokerController.getConsumerManager().registerConsumer(
            data.getGroupName(),
            clientChannelInfo,
            data.getConsumeType(),
            data.getMessageModel(),
            data.getConsumeFromWhere(),
            data.getSubscriptionDataSet(),
            isNotifyConsumerIdsChangedEnable
        );

        if (changed) {
            log.info("registerConsumer info changed {} {}",
                data.toString(),
                RemotingHelper.parseChannelRemoteAddr(ctx.channel())
            );
        }
    }

    //处理生产者心跳
    for (ProducerData data : heartbeatData.getProducerDataSet()) {
        this.brokerController.getProducerManager().registerProducer(data.getGroupName(),
            clientChannelInfo);
    }
    response.setCode(ResponseCode.SUCCESS);
    response.setRemark(null);
    return response;
}
```

**消费者的心跳最终调用`registerConsumer`判断消费者或者Topic订阅数据是否有改变，改变会通知组内消费者**

```java
public boolean registerConsumer(final String group, final ClientChannelInfo clientChannelInfo,
    ConsumeType consumeType, MessageModel messageModel, ConsumeFromWhere consumeFromWhere,
    final Set<SubscriptionData> subList, boolean isNotifyConsumerIdsChangedEnable) {

    //获取消费者组信息
    ConsumerGroupInfo consumerGroupInfo = this.consumerTable.get(group);
    if (null == consumerGroupInfo) {
        //没有就创建
        ConsumerGroupInfo tmp = new ConsumerGroupInfo(group, consumeType, messageModel, consumeFromWhere);
        ConsumerGroupInfo prev = this.consumerTable.putIfAbsent(group, tmp);
        consumerGroupInfo = prev != null ? prev : tmp;
    }

    //改变客户端消费者channel
    boolean r1 =
        consumerGroupInfo.updateChannel(clientChannelInfo, consumeType, messageModel,
            consumeFromWhere);
    //改变订阅数据
    boolean r2 = consumerGroupInfo.updateSubscription(subList);

    if (r1 || r2) {
        if (isNotifyConsumerIdsChangedEnable) {
            //如果消费者或组内订阅有变通知其他消费者
            this.consumerIdsChangeListener.handle(ConsumerGroupEvent.CHANGE, group, consumerGroupInfo.getAllChannel());
        }
    }

    //通知已注册 更新消息过滤器的订阅数据
    this.consumerIdsChangeListener.handle(ConsumerGroupEvent.REGISTER, group, subList);
    return r1 || r2;
}
```

**改变客户端channel实际上就是新增消费者的channel或覆盖已存在的channel，只有新增才算更改，才会后续通知**

**这里的channelInfoTable就是查询消费组下消费者ID会用到的**

```java
public boolean updateChannel(final ClientChannelInfo infoNew, ConsumeType consumeType,
    MessageModel messageModel, ConsumeFromWhere consumeFromWhere) {
    boolean updated = false;
    this.consumeType = consumeType;
    this.messageModel = messageModel;
    this.consumeFromWhere = consumeFromWhere;

    ClientChannelInfo infoOld = this.channelInfoTable.get(infoNew.getChannel());
    if (null == infoOld) {
        //没有就创建
        ClientChannelInfo prev = this.channelInfoTable.put(infoNew.getChannel(), infoNew);
        if (null == prev) {
            //以前也没有说明以前未注册过，改变成功
            log.info("new consumer connected, group: {} {} {} channel: {}", this.groupName, consumeType,
                messageModel, infoNew.toString());
            updated = true;
        }

        infoOld = infoNew;
    } else {
        //客户端channel存在，但id不相等 则覆盖ClientChannelInfo
        if (!infoOld.getClientId().equals(infoNew.getClientId())) {
            log.error("[BUG] consumer channel exist in broker, but clientId not equal. GROUP: {} OLD: {} NEW: {} ",
                this.groupName,
                infoOld.toString(),
                infoNew.toString());
            this.channelInfoTable.put(infoNew.getChannel(), infoNew);
        }
    }

    this.lastUpdateTimestamp = System.currentTimeMillis();
    infoOld.setLastUpdateTimestamp(this.lastUpdateTimestamp);

    return updated;
}
```

**更改订阅数据就是新增当前组内没有的订阅Topic，以及删除当前组内不在需要订阅的Topic**

```java
public boolean updateSubscription(final Set<SubscriptionData> subList) {
    boolean updated = false;
	//topic订阅数据
    for (SubscriptionData sub : subList) {
        SubscriptionData old = this.subscriptionTable.get(sub.getTopic());
        if (old == null) {
            SubscriptionData prev = this.subscriptionTable.putIfAbsent(sub.getTopic(), sub);
            if (null == prev) {
                //以前不存在则更新成功
                updated = true;
                log.info("subscription changed, add new topic, group: {} {}",
                    this.groupName,
                    sub.toString());
            }
        } else if (sub.getSubVersion() > old.getSubVersion()) {
            if (this.consumeType == ConsumeType.CONSUME_PASSIVELY) {
                log.info("subscription changed, group: {} OLD: {} NEW: {}",
                    this.groupName,
                    old.toString(),
                    sub.toString()
                );
            }
			//时间戳新则覆盖
            this.subscriptionTable.put(sub.getTopic(), sub);
        }
    }

    //遍历当前组的订阅数据
    Iterator<Entry<String, SubscriptionData>> it = this.subscriptionTable.entrySet().iterator();
    while (it.hasNext()) {
        Entry<String, SubscriptionData> next = it.next();
        String oldTopic = next.getKey();

        boolean exist = false;
        for (SubscriptionData sub : subList) {
            if (sub.getTopic().equals(oldTopic)) {
                exist = true;
                break;
            }
        }

        //如果当前订阅数据有但心跳时没有的Topic要删除
        if (!exist) {
            log.warn("subscription changed, group: {} remove topic {} {}",
                this.groupName,
                oldTopic,
                next.getValue().toString()
            );

            it.remove();
            updated = true;
        }
    }

    this.lastUpdateTimestamp = System.currentTimeMillis();

    return updated;
}
```

最后，**只有新增channel或订阅时才会调用`notifyConsumerIdsChanged`使用组内存储的客户端channel通知组内消费者**，请求码为`NOTIFY_CONSUMER_IDS_CHANGED`

**客户端收到这个请求会唤醒定时再平衡的线程去触发再平衡**

![Broker接收心跳](https://bbs-img.huaweicloud.com/blogs/img/20240913/1726215661927620814.png)

**注册生产者的流程也是去维护生产者客户端channel的，通过客户端channel便于RPC通信**，这里就不过多赘述









### 触发再平衡的时机

**触发再平衡机制是由RebalanceService循环定时触发的，默认情况下是等待20s触发一次**

```java
//默认等待20S
private static long waitInterval =
        Long.parseLong(System.getProperty(
            "rocketmq.client.rebalance.waitInterval", "20000"));

public void run() {
    while (!this.isStopped()) {
        //等待
        this.waitForRunning(waitInterval);
        //触发再平衡
        this.mqClientFactory.doRebalance();
    }
}
```



#### 消费者启动/上线通过定时任务触发再平衡

**DefaultMQPushConsumerImpl消费者在启动时，会启动MQClientInstance，而MQClientInstance会去启动再平衡的定时任务RebalanceService**

**但是RebalanceService会先等待再去触发再平衡，因此在消费者启动最后的步骤会调用`rebalanceImmediately`唤醒RebalanceService，从而触发再平衡**

```java
public synchronized void start() throws MQClientException {
    switch (this.serviceState) {
        case CREATE_JUST:
            //..
            
            //启动MQClientInstance 它会去启动RebalanceService
            mQClientFactory.start();
            this.serviceState = ServiceState.RUNNING;
            break;
        case RUNNING:
        case START_FAILED:
        case SHUTDOWN_ALREADY:
            throw new MQClientException("The PushConsumer service state not OK, maybe started once, "
                + this.serviceState
                + FAQUrl.suggestTodo(FAQUrl.CLIENT_SERVICE_NOT_OK),
                null);
        default:
            break;
    }

    this.updateTopicSubscribeInfoWhenSubscriptionChanged();
    this.mQClientFactory.checkClientInBroker();
    this.mQClientFactory.sendHeartbeatToAllBrokerWithLock();
    //唤醒RebalanceService
    this.mQClientFactory.rebalanceImmediately();
}
```

**流程：启动->开始再平衡定时任务->等待->被消费者启动最后代码唤醒->触发再平衡->新的消费者通知Broker->Broker通知组内其他消费者再平衡**





#### 消费者关闭/下线触发再平衡

消费者关闭时也会触发各种组件的关闭方法，其中有三个与触发消费者重新再平衡有关的操作，根据时间流程如下：

1. **mQClientFactory.shutdown：关闭MQClientInstance，会去关闭rebalanceService，从而唤醒rebalanceService触发再平衡**
2. **rebalanceImpl.destroy：清理 processQueueTable，触发再平衡后processQueue会更改从而会给Broker心跳**
3. **mQClientFactory.unregisterConsumer：注销消费者，给Broker心跳时会改变消费者，因此Broker会告诉其他消费者下线**

```java
//注销消费者 向broker发送注销请求 broker会给组内其他消费者下发再平衡
this.mQClientFactory.unregisterConsumer(this.defaultMQPushConsumer.getConsumerGroup());
```

请求码为`UNREGISTER_CLIENT`

```java
public void unregisterConsumer(final String group, final ClientChannelInfo clientChannelInfo,
    boolean isNotifyConsumerIdsChangedEnable) {
    ConsumerGroupInfo consumerGroupInfo = this.consumerTable.get(group);
    if (null != consumerGroupInfo) {
        //销毁channel
        consumerGroupInfo.unregisterChannel(clientChannelInfo);
        if (consumerGroupInfo.getChannelInfoTable().isEmpty()) {
            ConsumerGroupInfo remove = this.consumerTable.remove(group);
            if (remove != null) {
                this.consumerIdsChangeListener.handle(ConsumerGroupEvent.UNREGISTER, group);
            }
        }
        //通知组内再平衡
        if (isNotifyConsumerIdsChangedEnable) {
            this.consumerIdsChangeListener.handle(ConsumerGroupEvent.CHANGE, group, consumerGroupInfo.getAllChannel());
        }
    }
}
```

**流程：销毁消费者->Broker通知组内所有消费者再平衡**





#### Broker通知消费者改变

**消费者接收Broker通知组内消费者有改变时，又会去唤醒再平衡的线程，导致触发再平衡**

```java
public RemotingCommand notifyConsumerIdsChanged(ChannelHandlerContext ctx,
    RemotingCommand request) throws RemotingCommandException {
    //...
    
    //唤醒
    this.mqClientFactory.rebalanceImmediately();
    //
    return null;
}
```







#### MQClientInstance定时任务向Broker心跳

MQClientInstance有个默认30S的**定时任务会向Broker进行心跳，消费者有改动也可能导致Broker下发再平衡机制**

```java
this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

    @Override
    public void run() {
        try {
            MQClientInstance.this.cleanOfflineBroker();
            MQClientInstance.this.sendHeartbeatToAllBrokerWithLock();
        } catch (Exception e) {
            log.error("ScheduledTask sendHeartbeatToAllBroker exception", e);
        }
    }
}, 1000, this.clientConfig.getHeartbeatBrokerInterval(), TimeUnit.MILLISECONDS);
```

流程与触发时机总结如下图：

![触发再平衡的时机](https://bbs-img.huaweicloud.com/blogs/img/20240913/1726216228974405673.png)





### 上文遗留问题

上篇文章描述消费消息，在消费失败时会进行重试，此时会进行延时最终把消息投递到重试Topic上

那么重试Topic是如何触发再平衡，以及生成PullRequest继续走通后续拉取消息的流程呢？

**DefaultMQPushConsumerImpl在启动时，会调用`copySubscription`，集群模式下会将消费组的重试Topic加入rebalanceImpl的内部订阅中**

```java
case CLUSTERING:
    final String retryTopic = MixAll.getRetryTopic(this.defaultMQPushConsumer.getConsumerGroup());
    SubscriptionData subscriptionData = FilterAPI.buildSubscriptionData(retryTopic, SubscriptionData.SUB_ALL);
    this.rebalanceImpl.getSubscriptionInner().put(retryTopic, subscriptionData);
    break;
```

**后续遍历topic进行再平衡时，也会遍历重试Topic从而能够拉取重试队列的消息进行消费重试**







### 再平衡导致的问题

从再平衡机制的流程不难看出，它牺牲部分一致性来满足流程中不阻塞的可用性，从而达到最终一致性

**在程序启动、队列扩容/缩容、消费者上线/下线等场景下，都可能导致短暂的再平衡分配队列不一致的情况，从而导致消息会被延迟消费、可能被重复消费**

如果要确保再平衡分配队列完全一致，不出现重复消费的情况，就只能处理再平衡阶段时通过停止拉取、消费等工作，牺牲可用性来换取一致性

**虽然最终一致性的再平衡机制可能会出现短暂的负载不一致，但只需要消费者做幂等即可解决，而且再平衡期间满足可用性，不会影响性能**

当线上需要在业务高峰期进行大量队列扩容，增强消费能力时会触发再平衡机制，可能影响吞吐量从而导致性能下降

为了避免这种情况，可以新增topic、队列，在旧消费者组临时增加“转发消息”的消费者，将消息转发到新队列中实现水平扩容消费粒度









### 总结

**再平衡机制负责将队列负载均衡到消费者，是拉取消息、消费消息的前提**

**再平衡通过牺牲一定的一致性（频繁触发可能负载不一致）来满足可用性，以此达到最终一致性，期间可能出现消息重复消费，因此消费要做幂等**

**消费者触发再平衡时，先遍历订阅的Topic，并根据Topic进行再平衡，通过获取Topic下的所有队列，并向Broker获取同组的其他消费者，然后根据分配策略分配队列给当前消费者，再根据分配的队列更新ProcessQueue，如果ProcessQueue有更新则要维护MQ的流控并向所有Broker进行心跳**

**Broker收到心跳后更新消费者的channel与订阅，如果有新增则会向同组消费者下发再平衡请求**

**消费者上线/下线、队列的增加、减少都会触发组内消费者的再平衡，消费者的定时任务也会触发再平衡**

**如果多消费者组同时订阅相同的Topic（包括tag也相同），那么消费时会导致各消费者组都有消费者进行消息消费**



#### 最后（点赞、收藏、关注求求啦\~）

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜



## RocketMQ（七）：消费者如何保证顺序消费？

之前的文章已经描述过消费消息有并发、顺序两种方式，其中并发消费常用于无序消息中，而顺序消息用于有序消息

顺序消费是确保消息严格有序的前提，当需要确保消息有序时需要采用顺序消费，否则会可能打破消息的有序性

顺序消费较为复杂，会涉及到多种锁来保证顺序消费

本篇文章就来描述顺序消费的原理，来看看RocketMQ是如何保证顺序消费的，导图如下：

![本文导图](https://bbs-img.huaweicloud.com/blogs/img/20241108/1731058901772985108.png)

> 往期好文：

[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)

[RocketMQ（五）：揭秘高吞吐量并发消费原理](https://juejin.cn/post/7433066208825524243)

[RocketMQ（六）：Consumer Rebalanc原理（运行流程、触发时机、导致的问题）](https://juejin.cn/post/7433436835462791187)



### 顺序消费原理

#### 再平衡时加分布式锁

在顺序消费下，再平衡机制为了让每个队列都只分配到一个消费者，要向Broker获取该队列的分布式锁

再平衡更新ProcessQueue时，调用`updateProcessQueueTableInRebalance`新增时，如果是顺序的再平衡要先判断内存队列processQueue是否加分布式锁：

```java
		for (MessageQueue mq : mqSet) {
        	//本地没有则要新增
            if (!this.processQueueTable.containsKey(mq)) {
                //如果顺序 要判断队列是否加锁， 未获取说明其他消费者已经获取由它负责，这里跳过
                if (isOrder && !this.lock(mq)) {
                    continue;
                }

               //新增略
            }
        }
```



> RebalanceImpl.lock

加锁的流程：**获取Broker信息、调用lockBatchMQ向Broker申请给（批量）队列加分布式锁、将拿到锁队列对应的ProcessQueue设置为已加锁并更新获取锁的时间用于判断锁是否过期**

```java
public boolean lock(final MessageQueue mq) {
    //获取Broker
    FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), MixAll.MASTER_ID, true);
    if (findBrokerResult != null) {
        LockBatchRequestBody requestBody = new LockBatchRequestBody();
        requestBody.setConsumerGroup(this.consumerGroup);
        requestBody.setClientId(this.mQClientFactory.getClientId());
        requestBody.getMqSet().add(mq);

        try {
            //向Broker请求要加锁的队列 返回的结果就是获取到锁的队列
            Set<MessageQueue> lockedMq =
                this.mQClientFactory.getMQClientAPIImpl().lockBatchMQ(findBrokerResult.getBrokerAddr(), requestBody, 1000);
            
            //将获取到锁的队列对应的ProcessQueue的lock字段设置为true标识获取到分布式锁，并更新锁时间用于判断是否过期
            for (MessageQueue mmqq : lockedMq) {
                ProcessQueue processQueue = this.processQueueTable.get(mmqq);
                if (processQueue != null) {
                    processQueue.setLocked(true);
                    processQueue.setLastLockTimestamp(System.currentTimeMillis());
                }
            }

            boolean lockOK = lockedMq.contains(mq);
            log.info("the message queue lock {}, {} {}",
                lockOK ? "OK" : "Failed",
                this.consumerGroup,
                mq);
            return lockOK;
        } catch (Exception e) {
            log.error("lockBatchMQ exception, " + mq, e);
        }
    }

    return false;
}
```

同时在ConsumeMessageOrderlyService组件启动时会开启**定时任务（默认20S）调用`lockAll`向Broker获取当前负责processQueue的分布式锁(如果已持有则相当于续期)**







#### 顺序消费流程

前文已经说过：PullRequest（拉取消息）与MessageQueue、ProcessQueue（内存中存储、消费消息）一一对应，关系密切

在消费端流程中涉及三把锁，**MessageQueue的本地锁、ProcessQueue的分布式锁和本地锁**

**PullRequest拉取消息到ProcessQueue后会提交异步消费消息，封装ConsumeRequest提交到线程池，其集群模式下的顺序消费流程为：**

1. **获取该队列messageQueue本地锁加锁**（防止线程池并发消费同一队列）
2. **校验是否持有processQueue分布式锁，如果未持有调用 `tryLockLaterAndReconsume` 延迟尝试加processQueue分布式锁并提交消费请求**【未获取到锁，说明其他节点已获取分布式锁，当前节点先延时3s再进行消费（可能是再平衡机制将该队列分配给其他节点）】
3. **循环开始顺序消费消息，每次消费设置的最大消费消息数量，如果消费成功就循环消费，期间校验是否持有processQueue分布式锁，以及是否超时**（默认60S，超时延时提交消费请求，期间还会封装上下文和执行消费前后的钩子）
4. **真正调用消息监听器进行消息消费时，需要获取processQueue的本地锁**（再平衡如果将队列分配给其他消费者，会删除该队列，加锁防止在删除的过程中可能并发进行消费，防止多节点的重复消费）
5. **最后处理消费后的结果 `processConsumeResult`**

![顺序消费流程](https://bbs-img.huaweicloud.com/blogs/img/20240919/1726716563551574057.png)

```java
@Override
public void run() {
    if (this.processQueue.isDropped()) {
        log.warn("run, the message queue not be able to consume, because it's dropped. {}", this.messageQueue);
        return;
    }

    //获取该队列本地内存锁的锁对象
    final Object objLock = messageQueueLock.fetchLockObject(this.messageQueue);
    //队列加锁
    synchronized (objLock) {
        //集群模式要获取分布式锁并且不过期 否则调用tryLockLaterAndReconsume延迟处理
        if (MessageModel.BROADCASTING.equals(ConsumeMessageOrderlyService.this.defaultMQPushConsumerImpl.messageModel())
            || (this.processQueue.isLocked() && !this.processQueue.isLockExpired())) {
            final long beginTime = System.currentTimeMillis();
            //消费成功循环处理 直到失败延迟处理
            for (boolean continueConsume = true; continueConsume; ) {
				
                //再次校验 没获取锁 或者 超时 都会调用tryLockLaterAndReconsume延迟处理 
                //...

                //如果循环超时则延时提交消费请求，后续重新消费
                long interval = System.currentTimeMillis() - beginTime;
                if (interval > MAX_TIME_CONSUME_CONTINUOUSLY) {
                    ConsumeMessageOrderlyService.this.submitConsumeRequestLater(processQueue, messageQueue, 10);
                    break;
                }

                
                //每次最大消费消息数量
                final int consumeBatchSize =
                    ConsumeMessageOrderlyService.this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize();
                //从内存队列中获取要消费的消息
                List<MessageExt> msgs = this.processQueue.takeMessages(consumeBatchSize);
                //消息可能是重试的，重置为正常的topic
                defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, defaultMQPushConsumer.getConsumerGroup());
                if (!msgs.isEmpty()) {
                    //顺序上下文
                    final ConsumeOrderlyContext context = new ConsumeOrderlyContext(this.messageQueue);
					//执行后的状态
                    ConsumeOrderlyStatus status = null;

                    //构建上下文 行前的钩子..

                    long beginTimestamp = System.currentTimeMillis();
                    ConsumeReturnType returnType = ConsumeReturnType.SUCCESS;
                    boolean hasException = false;
                    try {
                        //消费前加processQueue本地锁
                        this.processQueue.getConsumeLock().lock();
                        if (this.processQueue.isDropped()) {
                            log.warn("consumeMessage, the message queue not be able to consume, because it's dropped. {}",
                                this.messageQueue);
                            break;
                        }

                        //顺序消费消息
                        status = messageListener.consumeMessage(Collections.unmodifiableList(msgs), context);
                    } catch (Throwable e) {
                        hasException = true;
                    } finally {
                        this.processQueue.getConsumeLock().unlock();
                    }

					//处理状态..
                    
                    //执行后的钩子..

                    ConsumeMessageOrderlyService.this.getConsumerStatsManager()
                        .incConsumeRT(ConsumeMessageOrderlyService.this.consumerGroup, messageQueue.getTopic(), consumeRT);

                    //处理消费后的状态
                    continueConsume = ConsumeMessageOrderlyService.this.processConsumeResult(msgs, status, context, this);
                } else {
                    continueConsume = false;
                }
            }
        } else {
            if (this.processQueue.isDropped()) {
                log.warn("the message queue not be able to consume, because it's dropped. {}", this.messageQueue);
                return;
            }

            ConsumeMessageOrderlyService.this.tryLockLaterAndReconsume(this.messageQueue, this.processQueue, 100);
        }
    }
}
```

顺序消费过程中部分流程也与并发消费类似，只不过需要通过加锁的方式保证顺序消费



> List<MessageExt> msgs = this.processQueue.takeMessages(consumeBatchSize);

processQueue 存储消息的内存队列是由两个TreeMap实现的，Key为消息的偏移量作为顺序，优先消费先持久化的消息（偏移量小）

**其中 `msgTreeMap` 存储拉取到内存的消息，`consumingMsgOrderlyTreeMap` 在顺序消费时才使用，取出消息时将消息存入该容器，消费失败时再将消息放回 `msgTreeMap` 后续重复进行消费**

```java
public List<MessageExt> takeMessages(final int batchSize) {
    List<MessageExt> result = new ArrayList<MessageExt>(batchSize);
    final long now = System.currentTimeMillis();
    try {
        //取出加写锁
        this.treeMapLock.writeLock().lockInterruptibly();
        this.lastConsumeTimestamp = now;
        try {
            if (!this.msgTreeMap.isEmpty()) {
                for (int i = 0; i < batchSize; i++) {
                    //取出首个节点 也就是偏移量小的消息
                    Map.Entry<Long, MessageExt> entry = this.msgTreeMap.pollFirstEntry();
                    if (entry != null) {
                        result.add(entry.getValue());
                        //加入consumingMsgOrderlyTreeMap
                        consumingMsgOrderlyTreeMap.put(entry.getKey(), entry.getValue());
                    } else {
                        break;
                    }
                }
            }

            if (result.isEmpty()) {
                consuming = false;
            }
        } finally {
            this.treeMapLock.writeLock().unlock();
        }
    } catch (InterruptedException e) {
        log.error("take Messages exception", e);
    }

    return result;
}
```



> ProcessQueue.commit

**处理消费成功的结果时会调用 `ProcessQueue.commit` 进行更新msgCount、msgSize等字段并清空consumingMsgOrderlyTreeMap，最后返回偏移量后续更新消费偏移量**

```java
public long commit() {
    try {
        this.treeMapLock.writeLock().lockInterruptibly();
        try {
            Long offset = this.consumingMsgOrderlyTreeMap.lastKey();
            //维护  msgCount、msgSize
            msgCount.addAndGet(0 - this.consumingMsgOrderlyTreeMap.size());
            for (MessageExt msg : this.consumingMsgOrderlyTreeMap.values()) {
                msgSize.addAndGet(0 - msg.getBody().length);
            }
            //清空
            this.consumingMsgOrderlyTreeMap.clear();
            if (offset != null) {
                return offset + 1;
            }
        } finally {
            this.treeMapLock.writeLock().unlock();
        }
    } catch (InterruptedException e) {
        log.error("commit exception", e);
    }

    return -1;
}
```



> ProcessQueue.makeMessageToConsumeAgain

**处理失败则会调用 `ProcessQueue.makeMessageToConsumeAgain` 将取出的消息重新放回msgTreeMap，延迟后再尝试消费**

```java
public void makeMessageToConsumeAgain(List<MessageExt> msgs) {
    try {
        this.treeMapLock.writeLock().lockInterruptibly();
        try {
            for (MessageExt msg : msgs) {
                //消息放回msgTreeMap
                this.consumingMsgOrderlyTreeMap.remove(msg.getQueueOffset());
                this.msgTreeMap.put(msg.getQueueOffset(), msg);
            }
        } finally {
            this.treeMapLock.writeLock().unlock();
        }
    } catch (InterruptedException e) {
        log.error("makeMessageToCosumeAgain exception", e);
    }
}
```



> processConsumeResult

`processConsumeResult`根据消费状态处理结果大致分成成功与失败的情况：

**如果是成功会取出消息偏移量并进行更新（调用`commit`）**

```java
//获取偏移量
commitOffset = consumeRequest.getProcessQueue().commit();
//更新
if (commitOffset >= 0 && !consumeRequest.getProcessQueue().isDropped()) {
    this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
}
```

更新流程与并发消费使用的组件相同（这里不再说明，之前的文章已描述），也是定时向Broker更新消费偏移量的

如果是失败则会把消息放回内存队列（调用`makeMessageToConsumeAgain`），然后再调用`submitConsumeRequestLater`延时提交消费请求（延时重新消费）

与并发消费不同的是：**并发消费延时会放回Broker并且随着消费失败延时时间也会增加，而顺序消费一直都在内存中延时重试，如果一直消费失败会“卡住”导致消息堆积**

总结顺序消费流程如下：

![顺序消费流程](https://bbs-img.huaweicloud.com/blogs/img/20240919/1726716563551574057.png)





#### Broker处理获取分布式锁

**broker维护mqLockTable的双层Map，其中第一层Key为消费者组名，第二层为队列，值为锁实体**

```java
ConcurrentMap<String/* group */, ConcurrentHashMap<MessageQueue, LockEntry>> mqLockTable 
```

**队列的锁实体字段包含持有锁的客户端ID和获取锁的时间，用于判断某客户端是否持有锁**

```java
static class LockEntry {
    private String clientId;
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();
}
```

获取分布式锁的请求码为`LOCK_BATCH_MQ`，Broker使用AdminBrokerProcessor调用RebalanceLockManager的tryLockBatch进行处理：

1. **遍历需要加锁的队列，调用`isLocked`判断消费者（客户端）是否已持有队列的锁**

   （获取到队列对应的锁实体，通过锁实体记录的客户端ID与当前客户端ID是否相同，持有锁时间是否过期（60S）来判断当前是否为持有锁的状态，如果持有锁相当于获取锁成功并更新获取锁的时间，加入加锁队列集合，否则加入未加锁队列集合）

2. **如果（客户端）有队列当前未持有锁，则要尝试获取锁**（操作期间为复合操作，broker使用本地锁保证原子性）

3. **获取队列对应的锁实体判断是否持有锁**（为空说明为第一次获取锁直接创建，isLocked比较客户端ID并且未过期才算获取锁，isExpired已过期也可以获取锁，其他情况就是别的客户端已经获取锁）

4. **返回获取锁的队列集合**

![Broker处理批量加锁](https://bbs-img.huaweicloud.com/blogs/img/20240919/1726716658640468572.png)

```java
public Set<MessageQueue> tryLockBatch(final String group, final Set<MessageQueue> mqs,
    final String clientId) {
    //加锁队列集合
    Set<MessageQueue> lockedMqs = new HashSet<MessageQueue>(mqs.size());
    //未加锁队列集合
    Set<MessageQueue> notLockedMqs = new HashSet<MessageQueue>(mqs.size());

    for (MessageQueue mq : mqs) {
        //判断队列上次加锁的客户端是否为当前客户端
        if (this.isLocked(group, mq, clientId)) {
            lockedMqs.add(mq);
        } else {
            notLockedMqs.add(mq);
        }
    }

    //如果有的队列上次加锁的客户端不是当前客户端 则要尝试加锁（该期间可能别的客户端还在持有锁）
    if (!notLockedMqs.isEmpty()) {
        try {
            //加锁保证原子性 （这里的锁是broker本地锁保证复合操作的原子性）
            this.lock.lockInterruptibly();
            try {
                //根据消费组拿到对应队列与锁
                ConcurrentHashMap<MessageQueue, LockEntry> groupValue = this.mqLockTable.get(group);
                if (null == groupValue) {
                    //初始化
                    groupValue = new ConcurrentHashMap<>(32);
                    this.mqLockTable.put(group, groupValue);
                }

                //遍历需要加锁的集合
                for (MessageQueue mq : notLockedMqs) {
                    LockEntry lockEntry = groupValue.get(mq);
                    //锁实体为空说明第一次获取直接创建
                    if (null == lockEntry) {
                        lockEntry = new LockEntry();
                        //设置获取锁的客户端ID （类似偏向锁）
                        lockEntry.setClientId(clientId);
                        groupValue.put(mq, lockEntry);
                    }

                    //如果持有锁的客户端ID相同并且未过期（60S）则更新持有锁时间
                    if (lockEntry.isLocked(clientId)) {
                        lockEntry.setLastUpdateTimestamp(System.currentTimeMillis());
                        //加锁成功加入
                        lockedMqs.add(mq);
                        continue;
                    }

                    //当前持有锁的客户端ID
                    String oldClientId = lockEntry.getClientId();
					
                    //如果已过期 则设置当前客户端为新获取锁的客户端
                    if (lockEntry.isExpired()) {
                        lockEntry.setClientId(clientId);
                        lockEntry.setLastUpdateTimestamp(System.currentTimeMillis());
                        //加锁成功加入
                        lockedMqs.add(mq);
                        continue;
                    }

					//其他情况 则有其他客户端已获取锁
                }
            } finally {
                this.lock.unlock();
            }
        } catch (InterruptedException e) {
            log.error("putMessage exception", e);
        }
    }

    return lockedMqs;
}
```

解锁流程同理，都是操作mqLockTable

分布式锁过期时间在服务端Broker与客户端（消费者）不同：

**客户端默认加锁时间为30S `rocketmq.client.rebalance.lockMaxLiveTime`**

**客户端定时任务默认每20S进行锁续期 `rocketmq.client.rebalance.lockInterval`**

**服务端默认加锁时间为60S `rocketmq.broker.rebalance.lockMaxLiveTime`**





### 总结

**顺序消费的流程与并发消息流程的类似，但为了确保消息有序、依次进行消费，期间会需要加多种锁**

**顺序消费流程中会先对messageQueue加本地锁，这是为了确保线程池执行ConsumeRequest任务时只有一个线程执行**

**然后检查要消费的队列processQueue是否持有分布式锁，这是为了确保再平衡机制时被多个节点的消费者重复消费消息**

**如果未持有分布式锁会向Broker尝试加锁，并延时提交消费请求，后续重试**

**如果持有分布式锁会开始循环消费，期间也会检查持有分布式锁、超时等情况，不满足条件就延时重试**

**监听器消费消息时，还要持有processQueue的本地锁，这是为了防止当前消费者不再负责该队列的情况下会删除，不加锁并发删除时会导致重复消费**

**使用消息监听器消费完消息后根据状态进行处理结果，如果成功则在内存中更新消费偏移量，后续再定时向Broker更新（与并发消费相同）**

**如果失败则会将消息放回processQueue并延时提交消费请求后续重试，与并发消费不同（并发消费失败会延时，重投入重试队列再进行重试或加入死信队列，而顺序消息是一直在内存中重试，会阻塞后续消息）**

**再平衡机制新增processQueue时，如果是顺序消费就会去尝试获取它的分布式锁（默认30S过期），并且有定时任务默认每20S进行分布式锁续期**

**Broker使用锁实体作为processQueue的分布式锁，记录持有分布式锁的客户端以及过期时间（默认60S过期）**

**每次需要对哪些队列进行加锁，只需要判断队列对应的锁实体客户端ID以及过期时间即可**



#### 最后（点赞、收藏、关注求求啦\~）

我是菜菜，热爱技术交流、分享与写作，喜欢图文并茂、通俗易懂的输出知识

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜









## RocketMQ（八）：轻量级拉取消费原理

前几篇文章，我们从DefaultMQPushConsumer从再平衡给消费者分配队列开始、再到消费者拉取消息、最后通过并发/顺序的方式消费消息，已经完全描述它的实现原理

虽然它取名为"Push"，但内部实现获取消息依旧是使用拉取的方式，只是增加了长轮询机制

这样取名只是为了想表达它的消息会被“推送”到消息监听器上，而我们只需要实现自己的消息监听器来处理消息

这篇文章我们使用“逆推”的思维，来看看消费者的另一个实现DefaultLitePullConsumer是如何实现轻量级拉取消费的

本文思维导图如下：

![思维导图](https://bbs-img.huaweicloud.com/blogs/img/20241113/1731462958628829804.png)

> 往期回顾：

[RocketMQ（七）：消费者如何保证顺序消费？](https://juejin.cn/post/7435651780329734207)

[RocketMQ（六）：Consumer Rebalanc原理（运行流程、触发时机、导致的问题）](https://juejin.cn/post/7433436835462791187)

[RocketMQ（五）：揭秘高吞吐量并发消费原理](https://juejin.cn/post/7433066208825524243)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)





### 使用DefaultLitePullConsumer

DefaultLitePullConsumer使用起来与DefaultMQPushConsumer略有不同

**它不需要再配置消息监听器（因为需要手动去拉取），启动后通过调用它的`poll`方法来手动拉取消息进行处理**

```java
			DefaultLitePullConsumer consumer = new DefaultLitePullConsumer();

            //根据配置文件set...
			consumer.setConsumerGroup(groupName);
            consumer.subscribe(topic, tag);
            consumer.setNamesrvAddr("127.0.0.1:9876");
            consumer.start();

            executor.execute(() -> {
                while (true) {
                    //拉取消息
                    List<MessageExt> poll = consumer.poll();
                    log.info("{}拉取消息:{}", groupName, poll);
                }
            });
```



### 实现原理

熟悉过DefaultMQPushConsumer的我们肯定对消费的整体流程不会陌生，无非就是需要做到以下三点：

1. 再分配机制如何给同组消费者负载均衡分配队列？
2. 如何拉取消息？
3. 如何消费消息？

授人以鱼不如授人以渔，这次我们直接以`poll`为入口，“逆推”其实现的原理

（面对没有文档、没有自顶向下的架构、不熟悉的源码都可以使用这种方式进行“推理”，找一个熟悉的业务实现点，往前寻找）



#### poll 获取消息

poll无参方法默认会携带5S的超时时间来进行调用，因此我们可以猜测如果没有消息到达就是每5s拉取一次消息

每个方法依次查看会发现它会进行**检查、自动提交、从内存中获取消息请求ConsumeRequest，最后再获取本次消费的消息以及维护数据**（更新偏移量、重置topic）

![poll获取消息](https://bbs-img.huaweicloud.com/blogs/img/20240920/1726819808748336886.png)

```java
public synchronized List<MessageExt> poll(long timeout) {
    try {
        //检查服务状态
        checkServiceState();
        //校验参数
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout must not be negative");
        }

        //自动提交 
        if (defaultLitePullConsumer.isAutoCommit()) {
            maybeAutoCommit();
        }
        long endTime = System.currentTimeMillis() + timeout;

        //获取消费请求
        ConsumeRequest consumeRequest = consumeRequestCache.poll(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

        //未超时
        if (endTime - System.currentTimeMillis() > 0) {
            //如果获取消费请求但processQueue已弃用要重新获取 直到超时
            while (consumeRequest != null && consumeRequest.getProcessQueue().isDropped()) {
                consumeRequest = consumeRequestCache.poll(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (endTime - System.currentTimeMillis() <= 0) {
                    break;
                }
            }
        }

        //获取到消费请求并且队列对应的processQueue未弃用 获取消息并维护其他状态
        if (consumeRequest != null && !consumeRequest.getProcessQueue().isDropped()) {
			//获取拉取到的消息
            List<MessageExt> messages = consumeRequest.getMessageExts();
            //内存队列中删除这批消息
            long offset = consumeRequest.getProcessQueue().removeMessage(messages);
            //更新偏移量
            assignedMessageQueue.updateConsumeOffset(consumeRequest.getMessageQueue(), offset);
            //If namespace not null , reset Topic without namespace.
            //消息重置topic 可能之前是从其他特殊队列出来的？
            this.resetTopic(messages);
            return messages;
        }
    } catch (InterruptedException ignore) {

    }

    return Collections.emptyList();
}
```



> DefaultLitePullConsumerImpl.commitAll

查看自动提交的方法可以发现：**在自动提交中，如果当前时间超过下次自动提交的时间（默认每隔5S）就会调用 `commitAll` **（从之前看过的源码，可以猜测这个Commit会去更新偏移量或持久化相关的操作）

```java
public synchronized void commitAll() {
    try {
        //遍历队列
        for (MessageQueue messageQueue : assignedMessageQueue.messageQueues()) {
            //获取队列的消费偏移量
            long consumerOffset = assignedMessageQueue.getConsumerOffset(messageQueue);
            if (consumerOffset != -1) {
                ProcessQueue processQueue = assignedMessageQueue.getProcessQueue(messageQueue);
                if (processQueue != null && !processQueue.isDropped()) {
                    //更新消费偏移量
                    updateConsumeOffset(messageQueue, consumerOffset);
                }
            }
        }
        
        //如果是广播模式则全部持久化
        if (defaultLitePullConsumer.getMessageModel() == MessageModel.BROADCASTING) {
            offsetStore.persistAll(assignedMessageQueue.messageQueues());
        }
    } catch (Exception e) {
        log.error("An error occurred when update consume offset Automatically.");
    }
}
```

虽然目前不太理解assignedMessageQueue队列是干嘛的，但从名字可以看出可能是再平衡给当前消费者负载均衡分配的队列

这个方法看上去**像兜底的更新消费偏移量（广播下持久化），某些情况下会遗漏，则每次获取消息时检查超时5S就进行兜底更新**

而`consumeRequestCache.poll` 从名称上看就像是专门存储consumeRequestCache的缓存

之前的文章也说过ConsumeRequest封装的消费请求，其中包含本次消费的消息列表以及对应的队列MessageQueue和ProcessQueue

```java
BlockingQueue<ConsumeRequest> consumeRequestCache = new LinkedBlockingQueue<ConsumeRequest>()
```

而存储ConsumeRequest的ConsumeRequestCache是阻塞队列，那就是明显的生产者、消费者模式

接下来只需要看看什么场景下会将ConsumeRequest放入阻塞队列，即可“逆推”出拉取消息的流程





#### pull 拉取消息

ConsumeRequestCache.put的方法只有一处就是提交消费请求

```java
private void submitConsumeRequest(ConsumeRequest consumeRequest) {
    try {
        consumeRequestCache.put(consumeRequest);
    } catch (InterruptedException e) {
        log.error("Submit consumeRequest error", e);
    }
}
```

而该方法在什么时机下会被调用，相信看过前几篇文章的同学们都很熟悉（即拉取到消息后），这个方法是在执行PullTaskImpl任务中找到消息后被调用的



> PullTaskImpl

通过名字也可以猜测PullTaskImpl任务就是拉取消息的任务

通过方法可以看出：**期间也会去做检查、流控，然后获取队列拉取偏移量进行拉取消息，拉到消息后将消息放入processQueue并封装消费请求进行提交，通知后续消息消费流程**

![拉取消息任务](https://bbs-img.huaweicloud.com/blogs/img/20240920/1726821176436802237.png)

```java
public void run() {
    if (!this.isCancelled()) {
        this.currentThread = Thread.currentThread();

        //队列暂停 延时1S重试
        if (assignedMessageQueue.isPaused(messageQueue)) {
            scheduledThreadPoolExecutor.schedule(this, PULL_TIME_DELAY_MILLS_WHEN_PAUSE, TimeUnit.MILLISECONDS);
            return;
        }

        //流控检查 不满足延时重试
        //...
        
        long offset = 0L;
        try {
            //获取队列下次拉取的偏移量
            offset = nextPullOffset(messageQueue);
        } catch (Exception e) {
            //失败延时重试
            scheduledThreadPoolExecutor.schedule(this, PULL_TIME_DELAY_MILLS_ON_EXCEPTION, TimeUnit.MILLISECONDS);
            return;
        }
        
        long pullDelayTimeMills = 0;
        try {
            SubscriptionData subscriptionData;
            String topic = this.messageQueue.getTopic();
            //订阅数据
            if (subscriptionType == SubscriptionType.SUBSCRIBE) {
                subscriptionData = rebalanceImpl.getSubscriptionInner().get(topic);
            } else {
                subscriptionData = FilterAPI.buildSubscriptionData(topic, SubscriptionData.SUB_ALL);
            }

            //拉取消息
            PullResult pullResult = pull(messageQueue, subscriptionData, offset, defaultLitePullConsumer.getPullBatchSize());
            if (this.isCancelled() || processQueue.isDropped()) {
                return;
            }
            switch (pullResult.getPullStatus()) {
                case FOUND:
                    //找到消息的情况 队列加锁
                    final Object objLock = messageQueueLock.fetchLockObject(messageQueue);
                    synchronized (objLock) {
                        if (pullResult.getMsgFoundList() != null && !pullResult.getMsgFoundList().isEmpty() && assignedMessageQueue.getSeekOffset(messageQueue) == -1) {
                            //拉取到的消息放入processQueue
                            processQueue.putMessage(pullResult.getMsgFoundList());
                            //封装消费请求并提交
                            submitConsumeRequest(new ConsumeRequest(pullResult.getMsgFoundList(), messageQueue, processQueue));
                        }
                    }
                    break;
                case OFFSET_ILLEGAL:
                    break;
                default:
                    break;
            }
            //更新拉取偏移量
            updatePullOffset(messageQueue, pullResult.getNextBeginOffset(), processQueue);
        } catch (InterruptedException interruptedException) {
        } catch (Throwable e) {
            pullDelayTimeMills = pullTimeDelayMillsWhenException;
        }
	
        
		if (!this.isCancelled()) {
            //未取消继续延时拉取
            scheduledThreadPoolExecutor.schedule(this, pullDelayTimeMills, TimeUnit.MILLISECONDS);
        } else {
        }
    }
}
```

**在通过队列获取偏移量时，优先使用seek手动设置的偏移量作为消费偏移量，然后再考虑拉取的偏移量，如果内存中拉取偏移量未设置要向broker获取**

```java
private long nextPullOffset(MessageQueue messageQueue) throws MQClientException {
    long offset = -1;
    //优先使用手动设置的偏移量
    long seekOffset = assignedMessageQueue.getSeekOffset(messageQueue);
    if (seekOffset != -1) {
        offset = seekOffset;
        //手动设置则将其更新为消费偏移量
        assignedMessageQueue.updateConsumeOffset(messageQueue, offset);
        assignedMessageQueue.setSeekOffset(messageQueue, -1);
    } else {
        //然后考虑拉取偏移量
        offset = assignedMessageQueue.getPullOffset(messageQueue);
        if (offset == -1) {
            //拉取偏移量未设置则向broker获取
            offset = fetchConsumeOffset(messageQueue);
        }
    }
    return offset;
}
```

**pull拉取消息的方法中，最终会采用同步的方式向Broker拉取数据（默认10S超时）`pullMessageSync`** （DefaultMQPushConsumer拉取消息采用的是异步）

（如果Broker没有消息的话也是长轮询机制的流程，有消息到达会拉取完再返回，长轮询机制在拉取消息的文章中也说过这里就不过多叙述）

**最终更新完偏移量，只要任务未被取消则会继续执行该任务** `scheduledThreadPoolExecutor.schedule(this, pullDelayTimeMills, TimeUnit.MILLISECONDS)`







#### 再平衡触发拉取消息任务

拉取流程也是类似的，只是有的细节实现不同，那么再来看看何时会触发PullTaskImpl任务

PullTaskImpl任务被构造的方法有两处：

1. seek手动更改偏移量时，构造PullTaskImpl任务后异步执行拉取消息
2. **再平衡机制触发**

**集群模式下会根据Topic进行再平衡，如果更新processQueue，队列需要更改时会调用`messageQueueChanged`**

（之前再平衡的文章分析过该流程，只是推和拉的具体实现不同，这里简单回顾下）

```java
//根据分配的队列
boolean changed = this.updateProcessQueueTableInRebalance(topic, allocateResultSet, isOrder);
if (changed) {
    //更改过后还需要更新队列 维护拉取任务
    this.messageQueueChanged(topic, mqSet, allocateResultSet);
}
```

**最终调用`updatePullTask`更新拉取任务，将不需要负责的队列任务取消，新增需要负责的队列任务启动**

```java
private void updatePullTask(String topic, Set<MessageQueue> mqNewSet) {
    Iterator<Map.Entry<MessageQueue, PullTaskImpl>> it = this.taskTable.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry<MessageQueue, PullTaskImpl> next = it.next();
        if (next.getKey().getTopic().equals(topic)) {
            if (!mqNewSet.contains(next.getKey())) {
                //取消不再负责队列的拉取任务
                next.getValue().setCancelled(true);
                it.remove();
            }
        }
    }
    //新增并启动需要负责队列的拉取任务
    startPullTask(mqNewSet);
}
```

**至此拉取消息的定时任务就会被再平衡机制给启动**

![再平衡更新拉取任务](https://bbs-img.huaweicloud.com/blogs/img/20240920/1726822683844255133.png)

**虽然拉取消息的任务是同步拉取，但是是放在线程池中执行的，并不会阻塞其他队列的拉取**

**向Broker更新消费偏移量也是相同的，MQClientInstance启动时开启默认5S的定时任务进行同步消费偏移量`MQClientInstance.this.persistAllConsumerOffset()`**









### 总结

**DefaultLitePullConsumer运行流程与推送的消费者类似，只是部分方法内部实现不同**

**再平衡机制会将队列负载均衡到消费者，同时更新队列对应的拉取任务**

**拉取任务使用线程池执行，拉取前会检查状态以及流控失败就延迟重试，然后获取下次拉取消息的偏移量，接着同步向broker进行拉取消息**

**如果拉取到消息，会将消息存储在队列对应的processQueue，并封装消费请求提交到ConsumerQueueCache中**

**拉取与推送的一大区别是，拉取获取消息的逻辑需要自己来实现，更加自由易扩展，poll获取消息则是从ConsumerQueueCache中获取消费请求并拿到消息再进行处理**





## RocketMQ（九）：延迟消息是如何实现的？

上个阶段已经从再平衡机制、拉取消息、并发/顺序消费消息来完全描述过消费者相关的消费原理

其中在并发消费的文章中曾说过：**并发消费失败时会采用重试机制，将重试的消息作为延迟消息投入延迟队列，经历延迟时间后再重新放回重试队列，等待后续被消费者拉取然后再进行重试消费**

其中，延迟消息（Delayed Message）不仅仅只用于重试，还是一个非常实用的功能，它允许消息在指定的时间后才被消费，这对于定时任务、订单超时提醒、促销活动等场景尤为重要

当时并没有详细说明延时队列的原理，本篇文章通过图文并茂、通俗易懂的说明延迟消息是如何实现的

阅读本篇文章之前需要了解[消息发送](https://juejin.cn/post/7412672656363798591)、[持久化](https://juejin.cn/post/7412922870521184256)相关的流程

本文导图如下：

![导图](https://bbs-img.huaweicloud.com/blogs/img/20241119/1731979252982926714.png)



> 往期回顾：

[RocketMQ（八）：轻量级拉取消费原理](https://juejin.cn/post/7436419198446387240)

[RocketMQ（七）：消费者如何保证顺序消费？](https://juejin.cn/post/7435651780329734207)

[RocketMQ（六）：Consumer Rebalanc原理（运行流程、触发时机、导致的问题）](https://juejin.cn/post/7433436835462791187)

[RocketMQ（五）：揭秘高吞吐量并发消费原理](https://juejin.cn/post/7433066208825524243)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)



### 使用延迟消息

使用延迟消息非常简单，只需要调用 `setDelayTimeLevel` 方法设置延迟的级别

```java
Message message = new Message(topic, tag, body);
message.setDelayTimeLevel(delayLevel);
```

一共分为18个延迟级别可以设置：

| 投递等级（delay level） | 延迟时间 | 投递等级（delay level） | 延迟时间 |
| :---------------------: | :------: | :---------------------: | :------: |
|            1            |    1s    |           10            |   6min   |
|            2            |    5s    |           11            |   7min   |
|            3            |   10s    |           12            |   8min   |
|            4            |   30s    |           13            |   9min   |
|            5            |   1min   |           14            |  10min   |
|            6            |   2min   |           15            |  20min   |
|            7            |   3min   |           16            |  30min   |
|            8            |   4min   |           17            |    1h    |
|            9            |   5min   |           18            |    2h    |

设置完延时级别后，其他使用方式与普通消息相同，延时消息的机制是在Broker自动实现的，等待对应的延时时间后，消息就会被重新进行消费



### 延迟消息原理

接下来让我们分析下，延时消息是如何实现的



#### 消息投入延时队列

`setDelayTimeLevel` 方法会**在消息的properties中对延时级别进行存储**

```java
public static final String PROPERTY_DELAY_TIME_LEVEL = "DELAY";

public void setDelayTimeLevel(int level) {
    this.putProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL, String.valueOf(level));
}
```

与普通消息发送消息流程相同，这里就不过多叙述，主要对延迟消息的处理在Broker对消息进行持久化时：

在持久化消息的流程中，需要对CommitLog进行追加消息数据 `this.commitLog.asyncPutMessage(msg)`

> CompletableFuture<PutMessageResult> putResultFuture = this.commitLog.asyncPutMessage(msg);

在此方法中，**CommitLog追加数据前会判断消息是否为延迟消息，如果是则会将消息原有的topic、队列信息替换为延迟相关的，并将原数据存储到properties**

```java
if (msg.getDelayTimeLevel() > 0) {
    if (msg.getDelayTimeLevel() > this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel()) {
        msg.setDelayTimeLevel(this.defaultMessageStore.getScheduleMessageService().getMaxDelayLevel());
    }

    //topic改为延迟topic
    topic = TopicValidator.RMQ_SYS_SCHEDULE_TOPIC;
    //队列改为延迟队列
    int queueId = ScheduleMessageService.delayLevel2QueueId(msg.getDelayTimeLevel());

    // Backup real topic, queueId
    //将原始topic、队列数据备份到properties
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_TOPIC, msg.getTopic());
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_QUEUE_ID, String.valueOf(msg.getQueueId()));
    msg.setPropertiesString(MessageDecoder.messageProperties2String(msg.getProperties()));

    //更新为延迟相关的topic、队列
    msg.setTopic(topic);
    msg.setQueueId(queueId);
}
```

**延迟的topic固定为`SCHEDULE_TOPIC_XXXX`**，`ScheduleMessageService.delayLevel2QueueId`方法中则是对延迟级别进行减一，说明18个级别对应的队列ID为0-17

```java
public static int delayLevel2QueueId(final int delayLevel) {
    return delayLevel - 1;
}
```

至此消息就会被持久化到对应的延迟队列中，后续**延时队列中消息都要经过一定的延时时间才会被组件ScheduleMessageService重新取出投入到原数据的队列中**（延时时间与级别对应）





#### ScheduleMessageService初始化

**组件ScheduleMessageService用于处理延时消息，初始化的步骤：**

1. **加载数据：加载每个延时队列对应的偏移量、解析延时级别对应延时时间、矫正延时偏移量**
2. **初始化执行定时任务的线程池**
3. **遍历延时队列并创建传递延时消息的任务放入线程池执行**（每个队列对应一个任务）
4. **启动定时持久化的任务**

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727516243568788117.png)

```java
public void start() {
    if (started.compareAndSet(false, true)) {
        //加载数据
        this.load();
        
        //初始化线程池
        this.deliverExecutorService = new ScheduledThreadPoolExecutor(this.maxDelayLevel, new ThreadFactoryImpl("ScheduleMessageTimerThread_"));
        if (this.enableAsyncDeliver) {
            this.handleExecutorService = new ScheduledThreadPoolExecutor(this.maxDelayLevel, new ThreadFactoryImpl("ScheduleMessageExecutorHandleThread_"));
        }
        
        //遍历延时队列（延时级别以及对应的延时时间），并创建任务放入线程池进行执行
        for (Map.Entry<Integer, Long> entry : this.delayLevelTable.entrySet()) {
            //延时级别
            Integer level = entry.getKey();
            //延时时间 ms
            Long timeDelay = entry.getValue();
            //延时偏移量
            Long offset = this.offsetTable.get(level);
            if (null == offset) {
                offset = 0L;
            }

            //创建任务放入线程池执行
            if (timeDelay != null) {
                //异步的情况
                if (this.enableAsyncDeliver) {
                    this.handleExecutorService.schedule(new HandlePutResultTask(level), FIRST_DELAY_TIME, TimeUnit.MILLISECONDS);
                }
                //同步
                this.deliverExecutorService.schedule(new DeliverDelayedMessageTimerTask(level, offset), FIRST_DELAY_TIME, TimeUnit.MILLISECONDS);
            }
        }

        //开启持久化定时任务
        this.deliverExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (started.get()) {
                        ScheduleMessageService.this.persist();
                    }
                } catch (Throwable e) {
                    log.error("scheduleAtFixedRate flush exception", e);
                }
            }
        }, 10000, this.defaultMessageStore.getMessageStoreConfig().getFlushDelayOffsetInterval(), TimeUnit.MILLISECONDS);
    }
}
```

在加载数据的方法中，主要是**从文件中加载各个队列的偏移量、解析各个延时级别对应的延时时间以及矫正偏移量**

（这里的偏移量其实就是延时队列的ConsumerQueue记录的偏移量，通过consumerQueue记录能够找到消息）

> ScheduleMessageService.load

```java
public boolean load() {
    //加载偏移量
    boolean result = super.load();
    //解析延时级别
    result = result && this.parseDelayLevel();
    //矫正偏移量
    result = result && this.correctDelayOffset();
    return result;
}
```

调用`super.load()`时，会**从broker配置的存储目录下`config/delayOffset.json`文件，读取每个队列偏移量的JSON数据，最终将数据填充到`offsetTable`**

(`this.offsetTable.putAll(delayOffsetSerializeWrapper.getOffsetTable())`)

文件内容如下：

```json
{
	"offsetTable":{1:1,2:2,3:12,4:12,5:14,6:15,7:149,8:1251,9:397,10:112,11:60,12:59,13:58,14:58,15:64,16:60,17:59,18:1129
	}
}
```





> ScheduleMessageService.parseDelayLevel

**解析延时级别主要就是将各个延时级别对应的时间解析为毫秒，并将结果存储到`delayLevelTable`**

```java
public boolean parseDelayLevel() {
    //时间单位转换
    HashMap<String, Long> timeUnitTable = new HashMap<String, Long>();
    timeUnitTable.put("s", 1000L);
    timeUnitTable.put("m", 1000L * 60);
    timeUnitTable.put("h", 1000L * 60 * 60);
    timeUnitTable.put("d", 1000L * 60 * 60 * 24);

    //默认写死的延时级别 "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h"
    String levelString = this.defaultMessageStore.getMessageStoreConfig().getMessageDelayLevel();
    try {
        String[] levelArray = levelString.split(" ");
        for (int i = 0; i < levelArray.length; i++) {
            String value = levelArray[i];
            String ch = value.substring(value.length() - 1);
            Long tu = timeUnitTable.get(ch);

            int level = i + 1;
            if (level > this.maxDelayLevel) {
                this.maxDelayLevel = level;
            }
            long num = Long.parseLong(value.substring(0, value.length() - 1));
            long delayTimeMillis = tu * num;
            //转换为ms后 写入 delayLevelTable
            this.delayLevelTable.put(level, delayTimeMillis);
            if (this.enableAsyncDeliver) {
                this.deliverPendingTable.put(level, new LinkedBlockingQueue<>());
            }
        }
    } catch (Exception e) {
        log.error("parseDelayLevel exception", e);
        log.info("levelString String = {}", levelString);
        return false;
    }

    return true;
}
```

**`correctDelayOffset`矫正偏移量则是判断偏移量是否超过逻辑队列ConsumerQueue最小/大值，超出则设置为最小/大值**

后续较为重要的流程就是遍历所有延时队列，创建DeliverDelayedMessageTimerTask任务交给线程池处理（默认同步情况下）





#### 定时转发延时消息

**DeliverDelayedMessageTimerTask任务用于定时转发延时消息，会判断当前队列的消息是否过期，如果过期会将消息重投到原topic、队列中，否则延时重试**

执行过程中，会调用`executeOnTimeup`，流程如下：

1. **获取延时队列对应的消费队列ConsumerQueue**（根据topic、队列id）
2. **根据偏移量获取ConsumerQueue对应位置的映射缓冲**（方便后续读consumer queue记录）
3. **遍历解析consumer queue记录，判断消息是否超时，超时则找到CommitLog上的消息，并恢复消息原始topic、队列，最后进行转发到对应队列，期间失败或其他情况延时重试**

（需要理解[ConsumerQueue文件](https://juejin.cn/post/7412922870521184256#heading-3)）

```java
public void executeOnTimeup() {
    //找到延时队列对应的（逻辑）消费队列 ConsumerQueue
    ConsumeQueue cq =
        ScheduleMessageService.this.defaultMessageStore.findConsumeQueue(TopicValidator.RMQ_SYS_SCHEDULE_TOPIC,
            delayLevel2QueueId(delayLevel));

    if (cq == null) {
        //没找到消费队列就延时处理
        this.scheduleNextTimerTask(this.offset, DELAY_FOR_A_WHILE);
        return;
    }

    //根据偏移量获取对应的映射缓冲 方便后续读
    SelectMappedBufferResult bufferCQ = cq.getIndexBuffer(this.offset);
    if (bufferCQ == null) {
        //没获取到 可能是偏移量有问题 纠正 延时执行
        long resetOffset;
        if ((resetOffset = cq.getMinOffsetInQueue()) > this.offset) {
            log.error("schedule CQ offset invalid. offset={}, cqMinOffset={}, queueId={}",
                this.offset, resetOffset, cq.getQueueId());
        } else if ((resetOffset = cq.getMaxOffsetInQueue()) < this.offset) {
            log.error("schedule CQ offset invalid. offset={}, cqMaxOffset={}, queueId={}",
                this.offset, resetOffset, cq.getQueueId());
        } else {
            resetOffset = this.offset;
        }

        this.scheduleNextTimerTask(resetOffset, DELAY_FOR_A_WHILE);
        return;
    }

    //获取到映射缓存的情况下
    long nextOffset = this.offset;
    try {
        int i = 0;
        ConsumeQueueExt.CqExtUnit cqExtUnit = new ConsumeQueueExt.CqExtUnit();
        //遍历consumer queue记录 
        for (; i < bufferCQ.getSize() && isStarted(); i += ConsumeQueue.CQ_STORE_UNIT_SIZE) {
            //读取数据 消息偏移量、大小、tag哈希值（延时消息该字段被利用延时后的时间）
            long offsetPy = bufferCQ.getByteBuffer().getLong();
            int sizePy = bufferCQ.getByteBuffer().getInt();
            long tagsCode = bufferCQ.getByteBuffer().getLong();

            //...
			
            long now = System.currentTimeMillis();
            //超过延迟到达时间就是延迟到达时间，否则就是now 会被延时处理
            long deliverTimestamp = this.correctDeliverTimestamp(now, tagsCode);
            //下个偏移量
            nextOffset = offset + (i / ConsumeQueue.CQ_STORE_UNIT_SIZE);
			
            //时间未到的情况下延时处理
            long countdown = deliverTimestamp - now;
            if (countdown > 0) {
                this.scheduleNextTimerTask(nextOffset, DELAY_FOR_A_WHILE);
                return;
            }

            //根据consumerqueue记录解析的偏移量和大小找到commit log上的消息
            MessageExt msgExt = ScheduleMessageService.this.defaultMessageStore.lookMessageByOffset(offsetPy, sizePy);
            if (msgExt == null) {
                continue;
            }

            //将延时消息延时相关属性清理，使用原来的topic、队列数据
            MessageExtBrokerInner msgInner = ScheduleMessageService.this.messageTimeup(msgExt);
            if (TopicValidator.RMQ_SYS_TRANS_HALF_TOPIC.equals(msgInner.getTopic())) {
                log.error("[BUG] the real topic of schedule msg is {}, discard the msg. msg={}",
                    msgInner.getTopic(), msgInner);
                continue;
            }

            //转发消息 默认同步
            boolean deliverSuc;
            if (ScheduleMessageService.this.enableAsyncDeliver) {
                deliverSuc = this.asyncDeliver(msgInner, msgExt.getMsgId(), nextOffset, offsetPy, sizePy);
            } else {
                deliverSuc = this.syncDeliver(msgInner, msgExt.getMsgId(), nextOffset, offsetPy, sizePy);
            }

            //失败延迟重试
            if (!deliverSuc) {
                this.scheduleNextTimerTask(nextOffset, DELAY_FOR_A_WHILE);
                return;
            }
        }

        
        nextOffset = this.offset + (i / ConsumeQueue.CQ_STORE_UNIT_SIZE);
    } catch (Exception e) {
        log.error("ScheduleMessageService, messageTimeup execute error, offset = {}", nextOffset, e);
    } finally {
        bufferCQ.release();
    }

    this.scheduleNextTimerTask(nextOffset, DELAY_FOR_A_WHILE);
}
```

**由于每条延时队列上需要延时的时间相同，并且消息入队的顺序为FIFO，因此判断超时只需要依次取出判断即可**

**无论是同步转发还是异步转发，最终会调用`deliverMessage`重新持久化消息`ScheduleMessageService.this.writeMessageStore.asyncPutMessage(msgInner)`**

至此延时消息的原理描述完毕，流程图如下：

![](https://bbs-img.huaweicloud.com/blogs/img/20240928/1727516248579103329.png)





### 总结

**使用延时消息只需要通过API设置延时级别，不同的延时级别对应不同的延时时间**

**broker将消息追加到commit log前会判断消息是否有设置延时级别，如果设置则说明为延时消息，会将原始topic、队列ID等数据存储在properties，并将消息topic、队列ID改为延时队列相关属性，最终消息会被持久化到延时队列**

**每个延时队列都有定时任务隔100ms进行检测，如果消息超时则通过consumer queue记录找到commitlog中的消息，并将其原始topic、队列ID等信息恢复，再调用持久化消息的API，相当于将消息重投到最开始设置的队列中**

**由于每个延时队列延时的时间相同，消息入队后，检测超时可以顺序检测，离超时时间越近的消息越前，如果有大量消息同时定时相同时间，处理流程可能会导致堆积从而影响定时精度**



#### 最后（点赞、收藏、关注求求啦\~）

我是菜菜，热爱技术交流、分享与写作，喜欢图文并茂、通俗易懂的输出知识

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜





## RocketMQ（十）：如何保证消息严格有序？

在某些业务场景中，MQ需要保证消息的顺序性，比如支付订单应该在创建订单之后进行

如果不使用保证顺序的手段，由于多队列、网络等因素可能会导致先处理支付订单的消息再处理创建订单的消息，这样就会导致处理失败

为了避免这样的情况发生，使用MQ时有必要保证消息的顺序性，在RocketMQ中通常**使用顺序发送消息和顺序消费消息来保证消息的顺序性**

本篇文章就来描述RocketMQ下如何确保可靠的消息顺序性，思维导图如下：

![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728548046376604323.png)

> 往期回顾：

[RocketMQ（九）：延迟消息是如何实现的？](https://juejin.cn/post/7438640461818708022)

[RocketMQ（八）：轻量级拉取消费原理](https://juejin.cn/post/7436419198446387240)

[RocketMQ（七）：消费者如何保证顺序消费？](https://juejin.cn/post/7435651780329734207)

[RocketMQ（六）：Consumer Rebalanc原理（运行流程、触发时机、导致的问题）](https://juejin.cn/post/7433436835462791187)

[RocketMQ（五）：揭秘高吞吐量并发消费原理](https://juejin.cn/post/7433066208825524243)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)



### 顺序发送

当队列全局只有一个时，消息全局有序，此时只需要确保为单个生产者发送（多个生产者同时发送无法预估消息到达的顺序）

或者先生产创建订单的消息再生产支付订单的消息（确保消息不丢）由于全局有序只能有一个队列，队列的压力过大，所以不经常使用

更通用的做法是使用**队列有序：在发送消息时通过一定的路由算法将需要有序的消息分发到同一个队列中，使用相同的队列保证有序性**

![](https://bbs-img.huaweicloud.com/blogs/img/20241009/1728464292095309613.png)

**当使用队列有序时也需要确保由一个生产者进行串行发送**（先创建订单再支付这种情况下多生产者也是可以的，因为创建/支付订单虽然生产者可能不同，但能确保消息到达的情况下，消息也是有序的，满足因果一致性）

在RocketMQ中提供顺序消息的API，与发送其他消息的方法类似，只是参数增加队列选择器MessageQueueSelector和分片键（通常是业务唯一ID）

```java
public void sendOrderMsg(String topic, String msg, SelectMessageQueueByHash selectMessageQueue, String orderId) {
    Message message = new Message(topic, msg.getBytes());
    message.setBuyerId(orderId);
    try {
        producer.send(message, selectMessageQueue, orderId);
    } catch (MQClientException | RemotingException | InterruptedException | MQBrokerException e) {
        throw new RuntimeException(e);
    }
}
```

通常可以选择**使用SelectMessageQueueByHash，通过对业务唯一ID进行哈希来分配队列，以此来达到消息的队列有序**

在之前的分析[发送消息](https://juejin.cn/post/7412672656363798591)的文章中曾说过发送消息通用流程，顺序消息调用的API虽然与普通消息通用的API不同（调用 `sendSelectImpl` ）

但流程类似：**获取topic、队列数据，根据队列选择器选择队列，期间检测超时，最终调用核心方法 `sendKernelImpl` 进行发送消息**

```java
private SendResult sendSelectImpl(
    Message msg,
    MessageQueueSelector selector,
    Object arg,
    final CommunicationMode communicationMode,
    final SendCallback sendCallback, final long timeout
) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
    long beginStartTime = System.currentTimeMillis();
    this.makeSureStateOK();
    //参数校验
    Validators.checkMessage(msg, this.defaultMQProducer);

    //获取topic
    TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
    if (topicPublishInfo != null && topicPublishInfo.ok()) {
        MessageQueue mq = null;
        try {
            //获取队列
            List<MessageQueue> messageQueueList =
             mQClientFactory.getMQAdminImpl().parsePublishMessageQueues(topicPublishInfo.getMessageQueueList());
            Message userMessage = MessageAccessor.cloneMessage(msg);
            String userTopic = NamespaceUtil.withoutNamespace(userMessage.getTopic(), mQClientFactory.getClientConfig().getNamespace());
            userMessage.setTopic(userTopic);

            //选择队列
            mq = mQClientFactory.getClientConfig().queueWithNamespace(selector.select(messageQueueList, userMessage, arg));
        } catch (Throwable e) {
            throw new MQClientException("select message queue threw exception.", e);
        }

        //超时判断
        long costTime = System.currentTimeMillis() - beginStartTime;
        if (timeout < costTime) {
            throw new RemotingTooMuchRequestException("sendSelectImpl call timeout");
        }
        if (mq != null) {
            //发送消息
            return this.sendKernelImpl(msg, mq, communicationMode, sendCallback, null, timeout - costTime);
        } else {
            throw new MQClientException("select message queue return null.", null);
        }
    }

    //失败 
    validateNameServerSetting();
    throw new MQClientException("No route info for this topic, " + msg.getTopic(), null);
}
```

**顺序发送API可以看成没有重试机制、可自定义选择队列的同步发送消息版本**

SelectMessageQueueByHash的实现也比较简单，就是根据唯一ID哈希模上队列

```java
public class SelectMessageQueueByHash implements MessageQueueSelector {

    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        int value = arg.hashCode() % mqs.size();
        if (value < 0) {
            value = Math.abs(value);
        }
        return mqs.get(value);
    }
}
```

总的来说**如果要保证顺序发送消息，可以通过业务唯一ID与分片算法将消息放到相同队列，避免并发发送消息**（无法预估到达队列顺序）





### 顺序消费

前文说过消费者消息消息时，为了全力以赴通常都是使用线程池进行并发消费的

当一批顺序消息被同时拉取到消费者时，如果由线程池并发进行消费也会导致消息的顺序性失效

因此在消费端也需要进行顺序消费，使用DefaultMQPushConsumer进行消费时，设置消息监听器为MessageListenerOrderly

在顺序消费的文章中也说过：**设置消息监听器为MessageListenerOrderly时，会通过多种加锁的方式保证消费者顺序消费队列中的消息**

但**如果消费发生失败会阻塞队列导致消息堆积**，因此需要注意特殊处理，比如重试次数超过阈值时就记录下来后续再处理

```java
consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
    try {
        for (MessageExt msg : msgs) {
            // 获取消息的重试次数
            int retryCount = msg.getReconsumeTimes();
            System.out.println("Message [" + msg.getMsgId() + "] is reconsumed " + retryCount + " times");

            //如果重试次数超过阈值 记录
            if (retryCount >= 3) {
                System.out.println("Message [" + msg.getMsgId() + "] add DB");
            }

            // 模拟消费失败
            if (retryCount < 3) {
                throw new RuntimeException("Consume failed");
            }

            // 消费成功
            System.out.println("Message [" + msg.getMsgId() + "] consumed successfully");
        }
        return ConsumeOrderlyStatus.SUCCESS;
    } catch (Exception e) {
        // 记录日志
        e.printStackTrace();
        // 返回重试状态
        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
    }
});
```

总的来说：**使用MessageListenerOrderly保证顺序消费时需要在失败重试时进行特殊处理，如果一直失败会阻塞队列导致消息堆积**







### 一致性

在架构篇中层说过，每个Master Broker都可能负责Topic下不同队列（如图中TopicA的4个队列分布在Broker A和B）

![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728543629904199034.png)

**当Broker发生宕机时队列数量会发生改变，可能会导致分片算法将同一ID的消息分发到不同队列从而导致极端情况影响消息的顺序性**

**如果队列数量不改变则投递到宕机中Broker的消息自然会失败，从而影响系统的可用性**

类似于CAP理论，当发生宕机（分区容错 Partition Tolerance）时，要么保证可用性（Availability），要么保证一致性（Consistency）

**如果要确保强一致性，需要将Topic中消息类型设置为FIFO，并开启NameServer中的配置 `orderMessageEnable` 和 `returnOrderTopicConfigToBroker` 为true**

![](https://bbs-img.huaweicloud.com/blogs/img/20241010/1728543216112704667.png)

**如果即要满足可用性、又要能支持队列数量的水平扩容，还需要确保消息严格顺序：**

1. **在分片算法上选择满足单调性(在队列数量动态发生变化时，已分配的key只会被分片到原队列或新队列)的算法**（一致性哈希）

2. **先进行扩容队列 -> 再消费旧队列中遗留的消息 -> 最后再开始消费所有队列，以此来保证消息的严格顺序**（因为单调性分片算法还是可能会导致key被分片到其他队列，为了保证严格有序，要将旧队列中遗留消息优先消费，再消费新队列）

**单调性：**（比如有A、B、C三个旧队列，key被分片到B队列，新增三个D、E、F队列，key只能被分配到B或D或E或F队列，而不会被分配到A或C队列中）





### 总结

**在需要保证消息有序的业务场景下，RocketMQ由顺序发送和顺序消费来保证消息顺序性**

**顺序发送通常使用队列有序来保证，将唯一的业务ID根据分片算法分发到对应的队列中，要注意避免并发发送，以免无法预估消息到达队列的先后顺序**

**顺序消费通常使用MessageListenerOrderly消息监听器，它通过加锁的方式顺序消费队列消息，保证消息有序消费但会降低消费吞吐量，并且失败会一直重试，可能导致消息堆积，需要特殊处理失败的情况**

**当Broker发生宕机时会导致队列数量发生改变，极端情况下会影响消息的顺序性，如果要保证强一致性需要设置Topic的消息类型为FIFO并开启NameServer中的配置 `orderMessageEnable` 和 `returnOrderTopicConfigToBroker` 为true**

**如果即要满足可用性、又要能支持队列数量的水平扩容，还需要确保消息严格顺序，可以在分片算法上选择满足单调性(在队列数量动态发生变化时，已分配的key只会被分片到原队列或新队列)的算法，并进行先扩容队列 -> 再消费旧队列中遗留的消息 -> 最后再开始消费所有队列，以此来保证消息的严格顺序**



#### 最后（点赞、收藏、关注求求啦\~）

我是菜菜，热爱技术交流、分享与写作，喜欢图文并茂、通俗易懂的输出知识

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以starred持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜







## RocketMQ（十一）：事务消息如何满足分布式一致性？

### 前言

在分布式系统中由于相关联的多个服务所在的数据库互相隔离，数据库无法使用本地事务来保证数据的一致性，因此需要使用分布式事务来保证数据的一致性

比如用户支付订单后，需要更改订单状态，还需要涉及其他服务的其他操作如：物流出货、积分变更、清空购物车等

由于它们数据所存储的数据库会互相隔离，当订单状态修改成功/失败时，其他服务对应的数据也需要修改成功/失败，否则就会出现数据不一致的情况

解决分布式事务常用的一种方案是使用MQ做补偿以此来达到数据的最终一致性，而**RocketMQ提供的事务消息能够简单、有效的解决分布式事务满足数据最终一致性**

在上面支付订单的案例中，主分支只需要修改订单状态，其他分支（出货、积分变更、清空购物车）都可以发送事务消息来达到数据最终一致性

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241011/1728627625702740501.png)

本篇文章通过分析源码来描述事务消息的原理以及使用方法，并总结使用时需要注意的地方，思维导图如下：

![本文导图](https://bbs-img.huaweicloud.com/blogs/img/20241217/1734407771495895452.png)

> 往期回顾：

[RocketMQ（十）：如何保证消息严格有序？](https://juejin.cn/post/7440842636230131764)

[RocketMQ（九）：延迟消息是如何实现的？](https://juejin.cn/post/7438640461818708022)

[RocketMQ（八）：轻量级拉取消费原理](https://juejin.cn/post/7436419198446387240)

[RocketMQ（七）：消费者如何保证顺序消费？](https://juejin.cn/post/7435651780329734207)

[RocketMQ（六）：Consumer Rebalanc原理（运行流程、触发时机、导致的问题）](https://juejin.cn/post/7433436835462791187)

[RocketMQ（五）：揭秘高吞吐量并发消费原理](https://juejin.cn/post/7433066208825524243)

[RocketMQ（四）：消费前如何拉取消息？（长轮询机制）](https://juejin.cn/post/7417635848987033615)

[RocketMQ（三）：面对高并发请求，如何高效持久化消息？（核心存储文件、持久化核心原理、源码解析）](https://juejin.cn/post/7412922870521184256)

[RocketMQ（二）：揭秘发送消息核心原理（源码与设计思想解析）](https://juejin.cn/post/7412672656363798591)

[RocketMQ（一）：消息中间件缘起，一览整体架构及核心组件](https://juejin.cn/post/7411686792342732841)





### 使用事务消息

事务消息拥有“半事务”的状态，在这种状态下即时消息到达broker也不能进行消费，直到主分支本地事务提交，事务消息才能被下游服务进行消费

使用事务消息的流程如下：

1. **生产者发送半事务消息**（消息到达broker后处于半事务状态，下游服务暂时无法消费）
2. **生产者执行本地事务**，无论本地事务成功(commit)还是失败(rollback)都要通知broker，如果成功则事务消息允许被消费，如果失败则丢弃事务消息
3. 在步骤2中，由于网络等缘故broker可能未接收到本地事务执行的结果，**当broker等待一定时间未收到状态时会自动回查状态**

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241011/1728629365039187765.png)

发送事务消息的生产者为TransactionMQProducer，TransactionMQProducer的使用与默认类似，只不过需要设置事务监听器TransactionListener

**事务监听器接口需要实现executeLocalTransaction用于执行本地事务和checkLocalTransaction用于broker回查本地事务状态**

```java
public interface TransactionListener {
    //执行本地事务
    LocalTransactionState executeLocalTransaction(final Message msg, final Object arg);
    //回查事务状态
    LocalTransactionState checkLocalTransaction(final MessageExt msg);
}
```

它们的结果LocalTransactionState有三个状态：COMMIT_MESSAGE 成功、ROLLBACK_MESSAGE 失败、UNKNOW 未知

当为未知状态时，后续还会触发回查，直到超过次数或者返回成功/失败

**调用 `sendMessageInTransaction` 发送事务消息，其中参数arg用于扩展，执行本地事务时会携带使用**

```java
public TransactionSendResult sendMessageInTransaction(final Message msg,final Object arg)
```

根据我们的情况写出TransactionListener的模拟代码

```java
public class OrderPayTransactionListener implements TransactionListener {
    //执行本地事务 其中参数arg传递的为订单ID
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object orderId) {
        try {
            //修改订单状态为已支付
            if (updatePayStatus((Long) orderId)) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        } catch (Exception e) {
            //log
            return LocalTransactionState.UNKNOW;
        }
        return LocalTransactionState.ROLLBACK_MESSAGE;
    }


    //回查状态
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        Long orderId = Long.valueOf(msg.getBuyerId());
        //查询订单状态是否为已支付
        try {
            if (isPayed(orderId)) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        } catch (Exception e) {
            //log
            return LocalTransactionState.UNKNOW;
        }

        return LocalTransactionState.ROLLBACK_MESSAGE;
    }
}
```

执行本地事务时如果成功修改订单状态就返回commit，回查状态时判断订单状态是否为已支付





### 事务消息原理



#### 发送事务消息

[前文](https://juejin.cn/post/7412672656363798591)分析过通用的发送消息流程，而 `sendMessageInTransaction` 发送消息调用通用的发送消息流程外，还会在期间多做一些处理：

1. **准备**（检查事务监听器、消息、清理延迟级别、标记事务消息为半事务状态、存储数据）
2. **通用同步发送消息流程 `sendDefaultImpl` **（校验参数、获取路由信息、选择队列、封装消息、netty rpc调用，期间检查超时、超时情况）
3. **获取发送消息结果，如果成功使用事务监听器执行本地事务 `executeLocalTransaction` **
4. **根据本地事务状态单向通知broker `endTransactionOneway` **（有回查机制无需考虑失败）

```java
public TransactionSendResult sendMessageInTransaction(final Message msg,
    final LocalTransactionExecuter localTransactionExecuter, final Object arg)
    throws MQClientException {
    //检查事务监听器
    TransactionListener transactionListener = getCheckListener();
    if (null == localTransactionExecuter && null == transactionListener) {
        throw new MQClientException("tranExecutor is null", null);
    }
    //清除延迟等级 使用事务消息就不能使用延迟消息
    // ignore DelayTimeLevel parameter
    if (msg.getDelayTimeLevel() != 0) {
        MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_DELAY_TIME_LEVEL);
    }
    //检查消息
    Validators.checkMessage(msg, this.defaultMQProducer);
    SendResult sendResult = null;
    //标记事务消息为半事务状态
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
    //存储生产者组
    MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, this.defaultMQProducer.getProducerGroup());
    try {
        //通用的发送消息流程
        sendResult = this.send(msg);
    } catch (Exception e) {
        throw new MQClientException("send message Exception", e);
    }
    
    LocalTransactionState localTransactionState = LocalTransactionState.UNKNOW;
    Throwable localException = null;
    switch (sendResult.getSendStatus()) {
        case SEND_OK: {
            try {
                if (sendResult.getTransactionId() != null) {
                    msg.putUserProperty("__transactionId__", sendResult.getTransactionId());
                }
                String transactionId = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
                if (null != transactionId && !"".equals(transactionId)) {
                    msg.setTransactionId(transactionId);
                }
                if (null != localTransactionExecuter) {
                    localTransactionState = localTransactionExecuter.executeLocalTransactionBranch(msg, arg);
                } else if (transactionListener != null) {
                    log.debug("Used new transaction API");
                    //成功执行本地事务
                    localTransactionState = transactionListener.executeLocalTransaction(msg, arg);
                }
                if (null == localTransactionState) {
                    localTransactionState = LocalTransactionState.UNKNOW;
                }

                if (localTransactionState != LocalTransactionState.COMMIT_MESSAGE) {
                    log.info("executeLocalTransactionBranch return {}", localTransactionState);
                    log.info(msg.toString());
                }
            } catch (Throwable e) {
                log.info("executeLocalTransactionBranch exception", e);
                log.info(msg.toString());
                localException = e;
            }
        }
        break;
        case FLUSH_DISK_TIMEOUT:
        case FLUSH_SLAVE_TIMEOUT:
        case SLAVE_NOT_AVAILABLE:
            //刷盘超时 或 从节点不可用 相当于失败
            localTransactionState = LocalTransactionState.ROLLBACK_MESSAGE;
            break;
        default:
            break;
    }
    
    try {
        //通知broker本地事务状态
        this.endTransaction(msg, sendResult, localTransactionState, localException);
    } catch (Exception e) {
        log.warn("local transaction execute " + localTransactionState + ", but end broker transaction failed", e);
    }
    
    //返回
    TransactionSendResult transactionSendResult = new TransactionSendResult();
    transactionSendResult.setSendStatus(sendResult.getSendStatus());
    transactionSendResult.setMessageQueue(sendResult.getMessageQueue());
    transactionSendResult.setMsgId(sendResult.getMsgId());
    transactionSendResult.setQueueOffset(sendResult.getQueueOffset());
    transactionSendResult.setTransactionId(sendResult.getTransactionId());
    transactionSendResult.setLocalTransactionState(localTransactionState);
    return transactionSendResult;
}
```

在发送的流程中主要会**在发送前做一些准备如标记半事务状态，然后进行同步发送，如果发送成功则会执行本地事务，最后单向通知broker本地事务的状态**

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728876242887122584.png)







#### broker存储事务消息

之前的文章也说过[消息到达后，broker存储消息的原理](https://juejin.cn/post/7412922870521184256)（先写CommitLog、再写其他文件）

**事务消息在消息进行存储前，会使用桥接器TransactionalMessageBridge调用 `parseHalfMessageInner` ，将消息topic改为半事务topic并存储原始topic、队列ID**（方便后续重新投入真正的topic）

```java
private MessageExtBrokerInner parseHalfMessageInner(MessageExtBrokerInner msgInner) {
    //存储真正的topic和队列ID
    MessageAccessor.putProperty(msgInner, MessageConst.PROPERTY_REAL_TOPIC, msgInner.getTopic());
    MessageAccessor.putProperty(msgInner, MessageConst.PROPERTY_REAL_QUEUE_ID,
        String.valueOf(msgInner.getQueueId()));
    msgInner.setSysFlag(
        MessageSysFlag.resetTransactionValue(msgInner.getSysFlag(), MessageSysFlag.TRANSACTION_NOT_TYPE));
    //设置本次要投入的topic为半事务Topic RMQ_SYS_TRANS_HALF_TOPIC
    msgInner.setTopic(TransactionalMessageUtil.buildHalfTopic());
    msgInner.setQueueId(0);
    msgInner.setPropertiesString(MessageDecoder.messageProperties2String(msgInner.getProperties()));
    return msgInner;
}
```

这样**半事务状态的事务消息就会被投入半事务topic的队列中，这样就能达到消费者无法消费半事务消息**（因为它们没被投入真实的队列中）

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728876791041470986.png)





#### broker接收本地事务状态通知

生产者发送完消息，无论成功还是失败都会通知broker本地事务状态

broker**使用EndTransactionProcessor处理`END_TRANSACTION`的请求**，其核心逻辑就是根据本地事务状态进行处理：

1. **如果成功根据CommitLog偏移量找到半事务消息，将其重投到真实的topic、队列中，最后再删除**
2. **如果失败根据CommitLog偏移量找到半事务消息进行删除**

```java
public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws
    RemotingCommandException {
    //构建通用响应
    final RemotingCommand response = RemotingCommand.createResponseCommand(null);
    //解析
    final EndTransactionRequestHeader requestHeader =
        (EndTransactionRequestHeader) request.decodeCommandCustomHeader(EndTransactionRequestHeader.class);

    //从节点直接响应失败
    if (BrokerRole.SLAVE == brokerController.getMessageStoreConfig().getBrokerRole()) {
        response.setCode(ResponseCode.SLAVE_NOT_AVAILABLE);
        return response;
    }

    
    //...
    
    
    OperationResult result = new OperationResult();
    //成功的情况
    if (MessageSysFlag.TRANSACTION_COMMIT_TYPE == requestHeader.getCommitOrRollback()) {
        //调用 getHalfMessageByOffset 根据commitLog偏移量获取半事务消息
        result = this.brokerController.getTransactionalMessageService().commitMessage(requestHeader);
        //找到半事务消息
        if (result.getResponseCode() == ResponseCode.SUCCESS) {
            //检查数据
            RemotingCommand res = checkPrepareMessage(result.getPrepareMessage(), requestHeader);
            if (res.getCode() == ResponseCode.SUCCESS) {
                //检查成功 
                MessageExtBrokerInner msgInner = endMessageTransaction(result.getPrepareMessage());
                msgInner.setSysFlag(MessageSysFlag.resetTransactionValue(msgInner.getSysFlag(), requestHeader.getCommitOrRollback()));
                msgInner.setQueueOffset(requestHeader.getTranStateTableOffset());
                msgInner.setPreparedTransactionOffset(requestHeader.getCommitLogOffset());
                msgInner.setStoreTimestamp(result.getPrepareMessage().getStoreTimestamp());
                //清理半事务标识
                MessageAccessor.clearProperty(msgInner, MessageConst.PROPERTY_TRANSACTION_PREPARED);
                //重新将消息投入真实topic、队列中
                RemotingCommand sendResult = sendFinalMessage(msgInner);
                if (sendResult.getCode() == ResponseCode.SUCCESS) {
                    //重投成功 删除事务消息
                    this.brokerController.getTransactionalMessageService().deletePrepareMessage(result.getPrepareMessage());
                }
                return sendResult;
            }
            return res;
        }
    } else if (MessageSysFlag.TRANSACTION_ROLLBACK_TYPE == requestHeader.getCommitOrRollback()) {
        //失败情况 也是调用 getHalfMessageByOffset 根据commitLog偏移量获取半事务消息
        result = this.brokerController.getTransactionalMessageService().rollbackMessage(requestHeader);
        if (result.getResponseCode() == ResponseCode.SUCCESS) {
            RemotingCommand res = checkPrepareMessage(result.getPrepareMessage(), requestHeader);
            if (res.getCode() == ResponseCode.SUCCESS) {
                //找到消息检查完就删除事务消息
                this.brokerController.getTransactionalMessageService().deletePrepareMessage(result.getPrepareMessage());
            }
            return res;
        }
    }
    response.setCode(result.getResponseCode());
    response.setRemark(result.getResponseRemark());
    return response;
}
```

**成功或失败（commit/rollback）的情况都会删除半消息，成功的情况会将消息投入原始队列中，后续进行消费**

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728877755529870983.png)

而还要一种无法确定是成功还是失败的情况，需要broker进行回查





#### broker回查机制

负责回查的组件是**TransactionalMessageCheckService：定期对半事务消息进行检查是否需要回查**（在broker启动初始化时进行初始化）

其检查回查会调用`this.brokerController.getTransactionalMessageService().check`

它会**遍历事务topic `RMQ_SYS_TRANS_HALF_TOPIC` 下的所有队列，循环取出半事务消息进行判断是否需要进行回查**

由于代码较多，这里总结性贴出关键代码：

1. **根据队列、偏移量取出半事务消息 `getHalfMsg`**
2. **超过检查次数（15）或最大存储时间（72h）就丢弃半事务消息 `resolveDiscardMsg`**
3. **将消息重投入半消息topic（避免消息丢失）`putBackHalfMsgQueue`**
4. **向生产者发送回查请求（请求码为CHECK_TRANSACTION_STATE）`resolveHalfMsg`**

```java
public void check(long transactionTimeout, int transactionCheckMax,AbstractTransactionalMessageCheckListener listener) {
    //遍历事务topic下的所有队列，循环取出半事务消息进行判断是否需要进行回查
    String topic = TopicValidator.RMQ_SYS_TRANS_HALF_TOPIC;
	Set<MessageQueue> msgQueues = transactionalMessageBridge.fetchMessageQueues(topic);
    for (MessageQueue messageQueue : msgQueues) {
        while (true) {
            //超出边界会退出 代码略
            
            //获取半事务消息 这里的参数i是半事务消息偏移量
            GetResult getResult = getHalfMsg(messageQueue, i);
           	MessageExt msgExt = getResult.getMsg();
            
            //needDiscard 超过最大检查次数 15次
            //needSkip  超过最大存储时间 72h
            if (needDiscard(msgExt, transactionCheckMax) || needSkip(msgExt)) {
                //丢弃半事务消息
                listener.resolveDiscardMsg(msgExt);
                //..
                continue;
            }
            
            //...
            
            //超过6s
            if (isNeedCheck) {
                //将消息重投入半消息队列
            	if (!putBackHalfMsgQueue(msgExt, i)) {
                	continue;
            	}
            	//向生产者发送回查的请求 CHECK_TRANSACTION_STATE
            	listener.resolveHalfMsg(msgExt);
            }
            
        }
    }
}
```

**请求回查并不会返回结果，生产者处理查询到事务状态后，再向broker发送单向的本地事务状态通知请求**（endTransactionOneway）

![image.png](https://bbs-img.huaweicloud.com/blogs/img/20241014/1728883719582293809.png)





#### 生产者处理回查请求

ClientRemotingProcessor 处理broker发送的回查请求CHECK_TRANSACTION_STATE

**ClientRemotingProcessor 调用 `checkTransactionState` 进行处理：**

1. **调用事务监听器回查本地事务的方法 `transactionListener.checkLocalTransaction`**
2. **调用`endTransactionOneway` 对broker进行通知本地事务状态结果**







### 总结

**涉及多服务的分布式事务，不追求强一致性的情况下，可考虑使用事务消息+重试的方式尽力达到最终一致性**

**使用时需要定义事务监听器执行本地事务和回查本地事务状态的方法，注意可能消费失败，重试多次后需要记录并特殊处理避免最终数据不一致**

**使用事务消息时无法设置延迟级别，发送前会将延迟级别清除**

**发送事务消息采用同步发送，在发送前会标记为半(事务)消息状态，在发送成功后会调用事务监听器执行本地事务，最后单向通知broker本地事务的状态**

**broker存储半（事务）消息前会更改它的topic、queueId，将其持久化到事务（半消息）topic中，以此来达到暂时不可以被消费的目的**

**broker接收本地事务状态通知时，如果是commit状态则将半（事务）消息重投入原始topic、队列中，以此来达到可以进行消费的目的，并且删除半（事务）消息，rollback状态也会删除半（事务）消息，只有未知状态的情况下不删除，等待后续触发回查机制**

**broker使用组件定期遍历事务（半消息）topic下的所有队列检查是否需要进行回查，遍历队列时循环取出半（事务）消息，如果超过检查最大次数（15）或超时（72h），则会丢弃消息；否则会将半（事务）消息放回队列，当事务消息超过6s时会触发回查机制，向produce发送检查事务状态的请求**

**produce收到回查请求后，调用事务监听器的检查事务状态方法，并又调用单向通知broker本地事务状态**



#### 最后（点赞、收藏、关注求求啦\~）

我是菜菜，热爱技术交流、分享与写作，喜欢图文并茂、通俗易懂的输出知识

本篇文章被收入专栏 [消息中间件](https://juejin.cn/column/7405771885327892532)，感兴趣的同学可以持续关注喔

本篇文章笔记以及案例被收入 [Gitee-CaiCaiJava](https://gitee.com/tcl192243051/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)、 [Github-CaiCaiJava](https://github.com/Tc-liang/CaiCaiJava/tree/master/MQ/SpringBoot-RocketMQ/src/main/java/com/caicai/springbootrocketmq)，除此之外还有更多Java进阶相关知识，感兴趣的同学可以star持续关注喔\~

有什么问题可以在评论区交流，如果觉得菜菜写的不错，可以点赞、关注、收藏支持一下\~

关注菜菜，分享更多技术干货，公众号：菜菜的后端私房菜

## 末尾

