package protobuf.test;

import protobuf.model.ProductProto;
import java.io.FileOutputStream;
import java.io.FileInputStream;

public class ProductApp {
    public static void main(String[] args) throws Exception {
        // 1. 创建一个 Product 实例
        ProductProto.Product product = ProductProto.Product.newBuilder()
                .setId(1001)
                .setName("无线鼠标")
                .setDescription("高精度无线鼠标，适用于办公和游戏")
                .setPrice(2599) // 单位：分
                .setAvailable(true)
                .build();


        // 2. 序列化到文件
        try (FileOutputStream fos = new FileOutputStream("product.bin")) {
            product.writeTo(fos);
            System.out.println("✅ Product 已序列化至 product.bin");
        }

        // 3. 从文件反序列化
        ProductProto.Product deserializedProduct;
        try (FileInputStream fis = new FileInputStream("product.bin")) {
            deserializedProduct = ProductProto.Product.parseFrom(fis);
            System.out.println("✅ 反序列化成功！");
        }

        // 4. 打印反序列化的对象
        System.out.println("📦 商品信息:");
        System.out.println("ID: " + deserializedProduct.getId());
        System.out.println("名称: " + deserializedProduct.getName());
        System.out.println("描述: " + deserializedProduct.getDescription());
        System.out.println("价格: " + (deserializedProduct.getPrice() / 100.0) + " 元");
        System.out.println("是否上架: " + (deserializedProduct.getAvailable() ? "是" : "否"));

//        byte[] bytes = product.toByteArray();

    }
}
