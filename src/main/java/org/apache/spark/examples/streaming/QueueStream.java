/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.examples.streaming;

import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public final class QueueStream {

	public static void main(String[] args) throws Exception {
		final SparkConf sparkConf = new SparkConf().setAppName("QueueStream");

		// Create the context
		final JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(1000));

		// Create the queue through which RDDs can be pushed to a QueueInputDStream
		final Queue<JavaRDD<Integer>> rddQueue = new LinkedList<>();

		// Create and push some RDDs into the queue
		final List<Integer> list = Lists.newArrayList();
		for (int i = 0; i < 1000; i++) {
			list.add(i);
		}

		for (int i = 0; i < 30; i++) {
			rddQueue.add(ssc.sparkContext().parallelize(list));
		}

		// Create the QueueInputDStream and use it do some processing
		final JavaDStream<Integer> inputStream = ssc.queueStream(rddQueue);
		final JavaPairDStream<Integer, Integer> mappedStream = inputStream.mapToPair(i -> new Tuple2<>(i % 10, 1));
		final JavaPairDStream<Integer, Integer> reducedStream = mappedStream.reduceByKey((i1, i2) -> i1 + i2);

		reducedStream.print();
		ssc.start();
		ssc.awaitTermination();
	}
}
