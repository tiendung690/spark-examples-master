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

package org.apache.spark.examples.mllib;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import scala.Tuple2;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Example using MLLib ALS from Java.
 */
public final class AlternatingLeastSquares {

	static class ParseRating implements Function<String, Rating> {
		private static final Pattern COMMA = Pattern.compile(",");

		@Override
		public Rating call(String line) {
			final String[] tok = COMMA.split(line);
			final int x = Integer.parseInt(tok[0]);
			final int y = Integer.parseInt(tok[1]);
			final double rating = Double.parseDouble(tok[2]);
			return new Rating(x, y, rating);
		}
	}

	static class FeaturesToString implements Function<Tuple2<Object, double[]>, String> {
		@Override
		public String call(Tuple2<Object, double[]> element) {
			return element._1() + "," + Arrays.toString(element._2());
		}
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("Usage: AlternatingLeastSquares <ratings_file> <rank> <iterations> <output_dir> [<blocks>]");
			System.exit(1);
		}

		final SparkConf sparkConf = new SparkConf().setAppName("AlternatingLeastSquares");
		final int rank = Integer.parseInt(args[1]);
		final int iterations = Integer.parseInt(args[2]);
		final String outputDir = args[3];
		int blocks = -1;
		if (args.length == 5) {
			blocks = Integer.parseInt(args[4]);
		}

		final JavaSparkContext sc = new JavaSparkContext(sparkConf);
		final JavaRDD<String> lines = sc.textFile(args[0]);
		final JavaRDD<Rating> ratings = lines.map(new ParseRating());

		final MatrixFactorizationModel model = ALS.train(ratings.rdd(), rank, iterations, 0.01, blocks);
		model.userFeatures().toJavaRDD().map(new FeaturesToString()).saveAsTextFile(outputDir + "/userFeatures");
		model.productFeatures().toJavaRDD().map(new FeaturesToString()).saveAsTextFile(outputDir + "/productFeatures");
		System.out.println("Final user/product features written to " + outputDir);

		sc.stop();
	}
}
