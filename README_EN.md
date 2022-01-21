# Spotify Experts
This project has been carried out as part of the course "Distributed Computing" at the University Carlos III of Madrid.

**Authors**

Beatriz Gil Hernando and María Huerga Espino.

**Description and main goals of the project**

This project aims to carry out a study of Spotify playlists; as well as to implement playlist creation and song addition operations. The main tools used for the development of this project have been the Spotify Java API [1], Spotify for developers, Java and Spark. 
We will find two different functionalities:

- Based on several playlists, these will be analysed and the artists will be extracted from each one of them; in order to obtain the common artists of the playlists together with their number of appearances in them. This study will be carried out using Spark.
- Extraction of the top 10 songs of some artists given by their id. Creation of a playlist and subsequent addition of the songs obtained previously.

The project consists of the following classes: SpotifyCrawler.java, SparkLib.java, NewPlaylist.java and the dependency file pom-xml. In the latter, we can find the necessary dependencies to be able to make use of the Java API of Spotify, Spark and Unirest among others.


	<dependencies>
		<dependency>
			<groupId>se.michaelthelin.spotify</groupId>
			<artifactId>spotify-web-api-java</artifactId>
			<version>7.0.0</version>
		</dependency>
		<!-- https://mvnrepository.com/artifact/org.apache.spark/spark-core -->
		<dependency>
			<groupId>org.apache.spark</groupId>
			<artifactId>spark-core_2.13</artifactId>
			<version>3.2.0</version>
		</dependency>

		<!-- https://mvnrepository.com/artifact/commons-codec/commons-codec -->
		<dependency>
			<groupId>commons-codec</groupId>
			<artifactId>commons-codec</artifactId>
			<version>1.12</version>
		</dependency>
		<!-- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind -->
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-databind</artifactId>
			<version>2.12.5</version>
		</dependency>
		<dependency>
			<groupId>com.konghq</groupId>
			<artifactId>unirest-java</artifactId>
			<version>3.12.0</version>
		</dependency>
	</dependencies>


**Detailed description of the project**

**Application**

Through the Spotify Web API [2], external applications retrieve content from Spotify, such as album and playlist data. First, in order to make requests to this API that involve a user's personal data, we need to create an application and register it through Spotify for Developers [3].


This application will provide us with the necessary data for the authorisation process and to be able to act as a Spotify client, the Client ID and the Client Secret. This will allow us to obtain an access token to be able to make requests later on. All this is done from the **clientCredentials_Sync()** function:

	public void clientCredentials_Sync() {
		try {
			spotifyApi = new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret)
					.setRedirectUri(redirectUri).build();
			clientCredentialsRequest = spotifyApi.clientCredentials().build();
			clientCredentials = clientCredentialsRequest.execute();

			clientCredentials.getAccessToken();
			spotifyApi.setAccessToken(clientCredentials.getAccessToken());

		} catch (IOException | SpotifyWebApiException | ParseException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

**Pulling information from the playlist**.

	public void getDailyMixAnalysis_Sync() {
		try {
			String playlistId[] = { "37i9dQZF1DWWIjcwuHgtIN", "37i9dQZF1DWSpF87bP6JSF", "37i9dQZEVXbNFJfN1Vw8d9"};
			String files[] = { "DailyMix_user1.txt", "DailyMix_user2.txt", "DailyMix_user3.txt" };
			for (int j = 0; j < playlistId.length; j++) {
				final GetPlaylistRequest getPlaylistRequest = spotifyApi.getPlaylist(playlistId[j]).build();
				final Playlist playlist = getPlaylistRequest.execute();
				File file = new File(files[j]);
				FileWriter script = new FileWriter(file);
				int length = playlist.getTracks().getItems().length;
				String[][] playlist_data = new String[length][3];

				for (int i = 0; i < length; i++) {
					list_data[i][0] = ((Track) playlist.getTracks().getItems()[i].getTrack()).getName();
					playlist_data[i][1] = String
							.valueOf((((Track) playlist.getTracks().getItems()[i].getTrack()).getArtists()[0].getName());
					playlist_data[i][2] = String
							.valueOf((((Track) playlist.getTracks().getItems()[i].getTrack()).getType());
					String singer = playlist_data[i][1] + "\n";
					write.write(singer);
				}
				write.close();
			}
		} catch (IOException | SpotifyWebApiException | ParseException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

The **getDailyMixAnalysis_Sync()** function will be in charge of analysing the playlists, providing a .txt file for each one of them with the artists that appear.

To implement this functionality, it is necessary to obtain the ids of the playlists we want to analyse. To do this, we must look for the playlist link, which will have a format similar to: https://open.spotify.com/playlist/{playlist_id}. In this way, we can see that the id of each playlist will be the digits that follow the playlist directory.

Once we have the ids, a request for the playlist is made to Spotify using the API. Once it has obtained the playlist, it extracts the name of the artist and the name of the song, among other data. Finally, all the artists obtained are written in a different file for each playlist, since this is the data we want to analyse with Spark.

**Analysis using Spark**

Once we have all the artists of all the playlists in txt files obtained from the previous functionality, we will go on to carry out an analysis to find the common artists between them. To do this we will use Spark, which will allow us to know the artists in common and the number of times they appear in each playlist. 

	public void sparkDailyMixAnalysis() {
		final String File_list[] = { "DailyMix_user1.txt", "DailyMix_user2.txt", "DailyMix_user3.txt" };

		JavaPairRDD<String, Integer> DailyMix_user1 = null, DailyMix_user2 = null, DailyMix_user3 = null;
		JavaPairRDD<String, Integer> reswappedPair = null;
		JavaRDD<String> input, words;
		JavaPairRDD<String, Integer> counts;
		JavaPairRDD<Integer, String> swappedPair;
		String inputFile;

		// Create a Java Spark Context
		JavaSparkContext sc = new JavaSparkContext("local[2]", "wordcount", System.getenv("SPARK_HOME"),
				System.getenv("JARS"));

		for (int i = 0; i < File_List.length; i++) {
			inputFile = File_list[i];
			input = null;
			try {
				input = sc.textFile(inputFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			words = input.flatMap(new FlatMapFunction<String, String>() {// separates words
				public Iterator<String> call(String x) {// return Arrays.
					return Arrays.asList(x.split("\n")).iterator();
				}
			});
			counts = words.mapToPair(new PairFunction<String, String, String, Integer>() {
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
		List<Tuple2<String, Tuple2<Tuple2<Integer, Integer>, Integer>, Integer>> var = Total.collect();
		try {
			FileWriter output = new FileWriter("DailyMixAnalysis.txt");
			output.write("Artist A B C");
			for (int i = 0; i < var.size(); i++) {
				output.write(var.get(i).productElement(0) + "\t" + var.get(i).productElement(1) + "\n");

			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

**sparkDailyMixAnalysis()** is in charge of collecting the files created in **getDailyMixAnalysis_Sync()** and performing the analysis by processing each of them with Spark. It tries to make a comparison between the txt data in order to keep the artists that appear in all the lists. Once this process is done, the artists are written in a txt with the number of times they appear in each of the playlists. The output of this functionality would look like this: 

```
> Artist A B C
> TINI ((1,2),1)
> Marc Segu? ((1,1),1)
> Nathy Peluso ((1,1),1)
> Justin Quiles ((1,1),2)
> Aitana ((1,1),1)
> C. Tangana ((1,1),1)
> Rauw Alejandro ((1,1),4)
> Sebastian Yatra ((1,2),2)
```



**Creating a playlist**

	public static void createPlaylist_Sync() {
		String body = "{"name" + ": \œ "CDIST FINAL BeaMaría",\r "\r "description".
				+ " "description": "Playlist for María Huerga and Beatriz Gil's final project in Distributed Computing. uc3m.\",\r\r\r"
				+ " "public": "false" + "}";
		String at = "BQB9J-4GLAUj3716evP8ayApdc1X4gaoZueG5dHFHRFRe2JCaKUk0aG4TGP66CDFVoPdqaTTUFuSxQbohWoEuWj2p2AvwbYWn- NzU0OOQhTEHeME4Zx7YioLFK7NmeRN1CeM0kXGLwTmlvf1HevbDqDHx4Z6YENzFnFLcyFNCDSOPQ-CcMinLrjr-2KQ1ow0JIFx27A"; 
		RequestBodyEntity postReq = Unirest.post("https://api.spotify.com/v1/users/USERNAME/playlists")
				.header("Accept", "application/json").header("host", "api.spotify.com")
				.header("Authorization", "Bearer " + at).header("Content-Type", "application/json").body(body);

		HttpResponse<String> res = postReq.asString();

		System.out.println(res.getStatus() + " " + res.getBody());
	}

The **createPlaylist_Sync()** function will be in charge of creating an empty playlist, which can be filled later. It is based on the Unirest library, whose function is to handle HTTP requests.

To implement this functionality we must fill the body of the request with information about the new playlist such as the name, description and the character of the playlist, which can be public or private.

The HTTP request type will be a POST to the _endopoint_ https://api.spotify.com/v1/users/{user_id}/playlists; in addition the headers "Accept: application/json", "host, api.spotify.com", "Content-Type: application/json" and "Authorization: Bearer at" will be added with the header() method.

In order to access Spotify's Web API HTTP requests we need to request an access token via the Spotify for developers page: https://developer.spotify.com/console/post-playlists/. What we've called "String at" in our code.
The "asString()" method is invoked for the request to be executed.

**Get the "top 10 tracks" of some artists**.

	
	public static String[] getArtistsTopTracks_Sync() {
		clientCredentials_Sync();
		String artistId[] = { "5Q30fhTc0Sl0Q4C5dOjhhQ", "21ttsUKZ3y2Hm6nduyvbAw" };
		spotifyApi = new SpotifyApi.Builder().setAccessToken(accessToken).build();

		String[] ids_songs1 = null;
		String[] ids_songs2 = null;
		for (int i = 0; i < artistId.length; i++) {
			getArtistsTopTracksRequest = spotifyApi.getArtistsTopTracks(artistId[i], countryCode).build();

			try {
				final Track[] tracks = getArtistsTopTracksRequest.execute();
				final int length = tracks.length;
				if (i == 0) { 
					ids_tracks1 = new String[length];
					for (int j = 0; j < length; j++) {
						ids_songs1[j] = ((Track) tracks[j]).getId();
					}
				}
				if (i == 1) {
					ids_songs2 = new String[length];
					for (int jj = 0; jj < length; jj++) {
						ids_songs2[jj] = ((Track) tracks[jj]).getId(); 
					}
				}
			} catch (IOException | SpotifyWebApiException | ParseException e) {
				System.out.println("Error: " + e.getMessage());
			}
		}
		
		String[] total_ids_songs = ArrayUtils.addAll(ids_songs1, ids_songs2);
		return total_ids_songs;

	}


In order to fill the previously created playlist, we will extract the 10 best songs of some artists of our choice. For this we have implemented the function **getArtistsTopTracks_Sync().**.

The selected artists will be given by their id, which can be obtained from the artist's URL: https://open.spotify.com/artist/{artist_id}.


First, we will invoke the clientCredentials_Sync() method in order to authenticate ourselves and thus be able to access Spotify data. Afterwards, we can make requests to the API (getArtistsTopTracks()) to extract the requested songs that we will save in their corresponding arrays (ids_cancionesX), we will concatenate them and finally, the function will return a list with all the songs extracted from the artists given by their ids (total_ids_canciones).

**Filling our playlist**

	public static void addTracks(String[] song) {

		String body = "{}";
		String at = "BQAWazpPq4E2BX_WTgk2fykB5xFqgpbLPgrtko7-mkHAz37u_EkTjjjgQB9T9DJ_Rt9zwXKsblMtPnQgTyqb7RJ5smUsQajl5HJhQQsEsukDffc68UGJ0zw6T- eok1yJgK-YsAYOdiVf2fNRs7HRbtSTJBTXkMmS-eQ4INp_-jlzuAl42-piL8F3pjm6omX9q_zrCUw";
		for (int i = 0; i < song.length; i++) {
			RequestBodyEntity postReq = Unirest
					.post("https://api.spotify.com/v1/playlists/{ID_PLAYLIST}/tracks?uris=spotify%3Atrack%3A"
							+ song[i])
					.header("Accept", "application/json").header("host", "api.spotify.com")
					.header("Authorization", "Bearer " + at).header("Content-Type", "application/json").body(body);

			HttpResponse<String> res = postReq.asString();
			System.out.println(res.getStatus() + " " + res.getBody());
		}

	}


Finally, we have all the necessary data to add the songs from the getArtistsTopTracks_Sync() function to our playlist created by the createPlaylist_Sync() function. To do so, we have implemented the function **addTracks(String[] song)**, whose input parameter will be the list of the ids of the songs obtained previously.
This function is similar to createPlaylist_Sync(); since we will also use the Unirest library, and thus be able to handle HTTP requests.

You will also need to request an access token from Spotify for Developers: https://developer.spotify.com/console/post-playlist-tracks/.

The HTTP request type will be a POST to _endopoint_ https://api.spotify.com/v1/playlists/{playlist_id}/tracks. You will be able to get the playlist id from its URL https://open.spotify.com/playlist/{playlist_id}.



**Bibliography**

[1] https://github.com/spotify-web-api-java/spotify-web-api-java

[2] Spotify Web API. https://developer.spotify.com/documentation/web-api/

[3] https://developer.spotify.com/dashboard/

