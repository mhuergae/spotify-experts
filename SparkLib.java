package cdistfinal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class SparkLib implements Serializable {

	private static final long serialVersionUID = 1L;

	public void sparkDailyMixAnalysis() {
		final String Lista_ficheros[] = { "DailyMix_user1.txt", "DailyMix_user2.txt", "DailyMix_user3.txt" };

		JavaPairRDD<String, Integer> DailyMix_user1 = null, DailyMix_user2 = null, DailyMix_user3 = null;
		JavaPairRDD<String, Integer> reswappedPair = null;
		JavaRDD<String> input, words;
		JavaPairRDD<String, Integer> counts;
		JavaPairRDD<Integer, String> swappedPair;
		String inputFile;

		// Create a Java Spark Context
		JavaSparkContext sc = new JavaSparkContext("local[2]", "wordcount", System.getenv("SPARK_HOME"),
				System.getenv("JARS"));

		for (int i = 0; i < Lista_ficheros.length; i++) {
			inputFile = Lista_ficheros[i];
			input = null;
			try {
				input = sc.textFile(inputFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			words = input.flatMap(new FlatMapFunction<String, String>() {// separa palabras
				public Iterator<String> call(String x) {
					return Arrays.asList(x.split("\n")).iterator();
				}
			});
			counts = words.mapToPair(new PairFunction<String, String, Integer>() {
				public Tuple2<String, Integer> call(String x) {
					return new Tuple2<String, Integer>(x, 1);
				}
			}).reduceByKey(new Function2<Integer, Integer, Integer>() {
				public Integer call(Integer x, Integer y) {
					return x + y;
				}
			});
			swappedPair = counts.mapToPair(new PairFunction<Tuple2<String, Integer>, Integer, String>() {
				@Override
				public Tuple2<Integer, String> call(Tuple2<String, Integer> item) throws Exception {
					return item.swap();
				}
			});

			swappedPair = swappedPair.sortByKey();

			reswappedPair = swappedPair.mapToPair(new PairFunction<Tuple2<Integer, String>, String, Integer>() {
				@Override
				public Tuple2<String, Integer> call(Tuple2<Integer, String> item) throws Exception {
					return item.swap();
				}
			});

			if (inputFile == "DailyMix_user1.txt") {
				DailyMix_user1 = reswappedPair;
			}
			if (inputFile == "DailyMix_user2.txt") {
				DailyMix_user2 = reswappedPair;
			}
			if (inputFile == "DailyMix_user3.txt") {
				DailyMix_user3 = reswappedPair;
			} else {
			}
			;

		}

		JavaPairRDD<String, Tuple2<Tuple2<Integer, Integer>, Integer>> Total = DailyMix_user1.join(DailyMix_user2)
				.join(DailyMix_user3);
		List<Tuple2<String, Tuple2<Tuple2<Integer, Integer>, Integer>>> var = Total.collect();
		try {
			FileWriter salida = new FileWriter("DailyMixAnalysis.txt");
			salida.write("Artista \t A  B  C\n");
			for (int i = 0; i < var.size(); i++) {
				salida.write(var.get(i).productElement(0) + "\t\t" + var.get(i).productElement(1) + "\n");

			}
			salida.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
