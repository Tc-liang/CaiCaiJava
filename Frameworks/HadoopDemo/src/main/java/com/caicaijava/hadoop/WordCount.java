package com.caicaijava.hadoop;

import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * 统计单词出现的次数
 */
public class WordCount {

    /**
     * 实现Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>
     */
    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        /**
         * map 转换为 <单词,1>
         *
         * @param key     输入KEY
         * @param value   输入VALUE
         * @param context
         * @throws IOException
         * @throws InterruptedException
         */
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                //输出 <单词,1> 比如 <CaiCai,1> 表示CaiCai出现1次
                context.write(word, one);
            }
        }
    }

    /**
     * 实现Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>
     */
    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            //统计次数
            int sum = 0;

            //经过Shuffle 会对相同key进行分区 比如<CaiCai,2> <CaiCai,1> <CaiCai,3>在一个分区
            //values就是次数的集合 <2,1,3> 分别出现2、1、3次，累加即可
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            //累加次数后输出 <>
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        //第一个参数为输入地址
        FileInputFormat.addInputPath(job, new Path(args[0]));
        //第二个参数为输出地址
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        //等待任务执行完成结束
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}