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

package org.apache.spark.examples;

import com.google.common.collect.Iterables;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Computes the PageRank of URLs from an input file. Input file should
 * be in format of:
 * URL         neighbor URL
 * URL         neighbor URL
 * URL         neighbor URL
 * ...
 * where URL and their neighbors are separated by space(s).
 */
public final class PageRank {
	private static final Pattern SPACES = Pattern.compile("\\s+");

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: PageRank <file> <number_of_iterations>");
			System.exit(1);
		}

		final SparkConf sparkConf = new SparkConf().setAppName("PageRank");
		final JavaSparkContext ctx = new JavaSparkContext(sparkConf);

		// Loads in input file. It should be in format of:
		//     URL         neighbor URL
		//     URL         neighbor URL
		//     URL         neighbor URL
		//     ...
		final JavaRDD<String> lines = ctx.textFile(args[0], 1);
		final int ITERATIONS = Integer.parseInt(args[1]);

		// Loads all URLs from input file and initialize their neighbors.
		final JavaPairRDD<String, Iterable<String>> links = lines.mapToPair(s -> {
			String[] parts = SPACES.split(s);
			return new Tuple2<>(parts[0], parts[1]);
		}).distinct().groupByKey().cache();

		// Loads all URLs with other URL(s) link to from input file and initialize ranks of them to one.
		JavaPairRDD<String, Double> ranks = links.mapValues(rs -> 1.0);

		// Calculates and updates URL ranks continuously using PageRank algorithm.
		for (int current = 0; current < ITERATIONS; current++) {
			// Calculates URL contributions to the rank of other URLs.
			JavaPairRDD<String, Double> contribs = links.join(ranks).values().flatMapToPair(s -> {
				int urlCount = Iterables.size(s._1);
				List<Tuple2<String, Double>> results = new ArrayList<>();
				for (String n : s._1) {
					results.add(new Tuple2<>(n, s._2() / urlCount));
				}
				return results;
			});

			// Re-calculates URL ranks based on neighbor contributions.
			ranks = contribs.reduceByKey((a, b) -> a + b).mapValues(sum -> 0.15 + sum * 0.85);
		}

		// Collects all URL ranks and dump them to console.
		final List<Tuple2<String, Double>> output = ranks.collect();
		for (Tuple2 tuple : output) {
			System.out.println(tuple._1() + " has rank: " + tuple._2() + ".");
		}

		ctx.stop();
	}
}
