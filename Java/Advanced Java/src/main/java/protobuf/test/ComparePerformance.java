package protobuf.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import protobuf.model.ProductJson;
import protobuf.model.ProductProto;

public class ComparePerformance {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // JSON 对象
        ProductJson jsonObj = new ProductJson();
        jsonObj.id = 1;
        jsonObj.name = "Protobuf手机";
        jsonObj.description = "性能强劲";
        jsonObj.price = 199900;
        jsonObj.available = true;

        // Protobuf 对象
        ProductProto.Product protoObj = ProductProto.Product.newBuilder()
                .setId(1)
                .setName("Protobuf手机")
                .setDescription("性能强劲")
                .setPrice(199900)
                .setAvailable(true)
                .build();

        int loop = 100_000;
        byte[] jsonBytes = null;
        byte[] protoBytes = null;

        // warmup
        for (int i = 0; i < 1000; i++) {
            mapper.writeValueAsBytes(jsonObj);
            protoObj.toByteArray();
        }

        // JSON 序列化
        long t1 = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            jsonBytes = mapper.writeValueAsBytes(jsonObj);
        }
        long t2 = System.nanoTime();

        // Protobuf 序列化
        long t3 = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            protoBytes = protoObj.toByteArray();
        }
        long t4 = System.nanoTime();

        // JSON 反序列化
        long t5 = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            mapper.readValue(jsonBytes, ProductJson.class);
        }
        long t6 = System.nanoTime();

        // Protobuf 反序列化
        long t7 = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            ProductProto.Product.parseFrom(protoBytes);
        }
        long t8 = System.nanoTime();

        System.out.println("==== 性能对比（" + loop + " 次） ====");
        System.out.println("✅ JSON    序列化耗时: " + (t2 - t1) / 1_000_000 + " ms");
        System.out.println("✅ Protobuf序列化耗时: " + (t4 - t3) / 1_000_000 + " ms");
        System.out.println("✅ JSON    反序列化耗时: " + (t6 - t5) / 1_000_000 + " ms");
        System.out.println("✅ Protobuf反序列化耗时: " + (t8 - t7) / 1_000_000 + " ms");

        System.out.println("📦 JSON 数据大小:     " + jsonBytes.length + " 字节");
        System.out.println("📦 Protobuf 数据大小: " + protoBytes.length + " 字节");
    }
}
