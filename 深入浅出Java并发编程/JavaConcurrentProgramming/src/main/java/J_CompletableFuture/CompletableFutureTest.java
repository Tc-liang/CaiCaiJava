package J_CompletableFuture;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author: cl
 * @create: 2024/6/28 13:47
 * @description:
 */
public class CompletableFutureTest {

    public static final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);


    public static void main(String[] args) {
//        test();
//        testSync();
        exception();

        threadPool.shutdown();
    }

    public static void test() {

        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(() -> {
            System.out.println("task a run");
            return "a";
        }).exceptionally(e -> {
            System.out.println("出现异常");
            throw new RuntimeException("error");
        });


        CompletableFuture<String> taskB = taskA.thenApply((s) -> {
            System.out.println("task b run");
            return s + "b";
        }).exceptionally(e -> {
            System.out.println("出现异常");
            throw new RuntimeException("error");
        });


        CompletableFuture<String> taskC = CompletableFuture.supplyAsync(() -> {
            System.out.println("task c run");
            return "c";
        }).exceptionally(e -> {
            System.out.println("出现异常");
            throw new RuntimeException("error");
        });


        CompletableFuture<String> taskD = taskB.thenCombineAsync(taskC, (b, c) -> {
            System.out.println("task d run");
            return b + c;
        }).exceptionally(e -> {
            System.out.println("出现异常");
            throw new RuntimeException("error");
        });


        CompletableFuture<String> taskE = CompletableFuture.supplyAsync(() -> {
            System.out.println("task e run");
            return "a";
        }).exceptionally(e -> {
            System.out.println("出现异常");
            throw new RuntimeException("error");
        });


        CompletableFuture<String> taskF = CompletableFuture.supplyAsync(() -> {
            System.out.println("task f run");
            return "a";
        }).exceptionally(e -> {
            System.out.println("出现异常");
            throw new RuntimeException("error");
        });


        CompletableFuture.allOf(taskF, taskE, taskD);


//        CompletableFuture.anyOf()
    }


    public static void testSync() {
        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(() -> {
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            return "ok";
        }, threadPool);


        CompletableFuture<Void> taskB = taskA.thenAccept(s -> {
            //任务A执行完（不睡时）由当前线程执行
            //任务A未执行完（睡眠时）由线程池的工作线程执行
            System.out.println(s);
            System.out.println(s);
        });

        taskB.join();
    }


    public static void exception() {
        CompletableFuture<Void> taskException = CompletableFuture.supplyAsync(() -> {
            System.out.println("begin");
            return null;
        });

        taskException
                .thenApply(result -> {
                    int i = 1 / 0;
                    return i;
                })
                .exceptionally(err -> {
                    //java.util.concurrent.CompletionException: java.lang.ArithmeticException: / by zero
                    System.out.println(err);

                    //java.lang.ArithmeticException: / by zero
                    System.out.println(err.getCause());

                    //java.lang.ArithmeticException: / by zero
                    System.out.println(getException(err));
                    return 0;
                });

    }
    public static Throwable getException(Throwable throwable) {
        if ((throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && Objects.nonNull(throwable.getCause())) {
            return throwable.getCause();
        }
        return throwable;
    }
}
