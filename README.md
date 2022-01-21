# Spotify Experts
Este proyecto ha sido realizado como parte de la asignatura "Computación Distribuida" de la Universidad Carlos III de Madrid.

**Autoría**

Beatriz Gil Hernando y María Huerga Espino.

**Descripción y objetivo del proyecto**

Este proyecto tiene como objetivo realizar un estudio de una serie de listas de reproducción de Spotify (playlists); así como implementar operaciones de creación de playlists y añadido de canciones. Las herramientas principales utilizadas para el desarrollo de este proyecto han sido la API de Java de Spotify [1], Spotify para desarrolladores, Java y Spark. 
Encontraremos dos funcionalidades diferentes:

- Basándonos en varias playlists, estas serán analizadas y serán extraídos los artistas de cada una de ellas; para así, poder obtener los artistas comunes de las playlists junto con su número de apariciones en estas. Este estudio se realizará mediante Spark.
- Extracción de las 10 canciones top de unos artistas dados por su id. Creación de una playlist y posterior adicionado de las canciones obtenidas anteriormente.

El proyecto consta de las clases: SpotifyCrawler.java, SparkLib.java, NewPlaylist.java y el archivo de dependencias pom-xml. En este último, podremos encontrar las dependencias necesarias para poder hacer uso de la API de Java de Spotify, Spark y Unirest entre otras.


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


**Descripción detallada del proyecto**

**Aplicación**

A través de la API Web de Spotify [2], las aplicaciones externas recuperan contenido de Spotify, como datos de álbumes y listas de reproducción. En primer lugar, para poder realizar peticiones a esta API que involucren datos personales de un usuario debemos crear una aplicación y registrarla a través de Spotify for Developers [3].


Esta aplicación nos proporcionará los datos necesarios para el proceso de autorización y poder actuar como cliente de Spotify, el Client ID y el Client Secret. Esto nos permitirá obtener un token de acceso para poder realizar peticiones posteriormente. Todo esto se realiza desde la función **clientCredentials_Sync()**:

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

**Extracción de información de la playlist**

	public void getDailyMixAnalysis_Sync() {
		try {
			String playlistId[] = { "37i9dQZF1DWWIjcwuHgtIN", "37i9dQZF1DWSpF87bP6JSF", "37i9dQZEVXbNFJfN1Vw8d9"};
			String ficheros[] = { "DailyMix_user1.txt", "DailyMix_user2.txt", "DailyMix_user3.txt" };
			for (int j = 0; j < playlistId.length; j++) {
				final GetPlaylistRequest getPlaylistRequest = spotifyApi.getPlaylist(playlistId[j]).build();
				final Playlist playlist = getPlaylistRequest.execute();
				File fichero = new File(ficheros[j]);
				FileWriter escritura = new FileWriter(fichero);
				int longitud = playlist.getTracks().getItems().length;
				String[][] datos_lista = new String[longitud][3];

				for (int i = 0; i < longitud; i++) {
					datos_lista[i][0] = ((Track) playlist.getTracks().getItems()[i].getTrack()).getName();
					datos_lista[i][1] = String
							.valueOf(((Track) playlist.getTracks().getItems()[i].getTrack()).getArtists()[0].getName());
					datos_lista[i][2] = String
							.valueOf(((Track) playlist.getTracks().getItems()[i].getTrack()).getType());
					String cantante = datos_lista[i][1] + "\n";
					escritura.write(cantante);
				}
				escritura.close();
			}
		} catch (IOException | SpotifyWebApiException | ParseException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

La función **getDailyMixAnalysis_Sync()** será la encargada de realizar el análisis de las playlists, proporcionando un archivo .txt por cada una de ellas con los artistas que aparecen.

Para implementar esta funcionalidad, es necesario obtener los ids de las playlist que queramos analizar. Para ello debemos buscar el enlace de la playlist, que tendrá un formato similar a: https://open.spotify.com/playlist/{playlist_id}. De tal manera, podemos observar que el id de cada playlist serán los dígitos que van seguidos al directorio playlist.

Una vez tenemos los ids, se realiza una petición de las playlist a Spotify utilizando la API. Cuando ya ha obtenido la playlist, extrae el nombre del artista y el nombre de la canción entre otros datos. Finalmente, se escriben en un fichero diferente para cada playlist todos los artistas obtenidos; puesto que es el dato que queremos analizar con Spark.

**Análisis mediante Spark**

Una vez tenemos los artistas de todas las playlist en archivos txt obtenidos de la funcionalidad anterior, pasaremos a realizar un análisis para conseguir los artistas comunes entre ellas. Para ello utilizaremos Spark, que nos permitirá saber los artistas en común y el número de veces que aparecen en cada playlist. 

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

**sparkDailyMixAnalysis()** se encarga de recoger los ficheros creados en **getDailyMixAnalysis_Sync()** y realizar el análisis procesando cada uno de ellos con Spark. Trata de realizar una comparación entre los datos de los txt con la finalidad de quedarse con los artistas que aparecen en todas las listas. Una vez realizado este proceso se escribe en un txt los artistas con el número de veces que aparecen en cada una de las playlist. La salida de esta funcionalidad tendría esta forma: 

```
> Artista 		 A  B  C
> TINI			((1,2),1)
> Marc Segu?		((1,1),1)
> Nathy Peluso		((1,1),1)
> Justin Quiles		((1,1),2)
> Aitana		((1,1),1)
> C. Tangana		((1,1),1)
> Rauw Alejandro	((1,1),4)
> Sebastian Yatra	((1,2),2)
```



**Creación de una playlist**

	public static void createPlaylist_Sync() {
		String body = "{\r\n" + "  \"name\": \"CDIST FINAL BeaMaría\",\r\n"
				+ "  \"description\": \"Playlist para el proyecto final de Computación Distribuida de María Huerga y Beatriz Gil. uc3m.\",\r\n"
				+ "  \"public\": false\r\n" + "}";
		String at = "BQB9J-4GLAUj3716evP8ayApdc1X4gaoZueG5dHFHRFRe2JCaKUk0aG4TGP66CDFVoPdqaTUFuSxQbohWoEuWj2p2AvwbYWn-NzU0OOQhTEHeME4Zx7YioLFK7NmeRN1CeM0kXGLwTmlvf1HevbDqDHx4Z6YENzFnFLcyFNCDSOPQ-CcMinLrjr-2KQ1ow0JIFx27A"; 
		RequestBodyEntity postReq = Unirest.post("https://api.spotify.com/v1/users/USERNAME/playlists")
				.header("Accept", "application/json").header("host", "api.spotify.com")
				.header("Authorization", "Bearer " + at).header("Content-Type", "application/json").body(body);

		HttpResponse<String> res = postReq.asString();

		System.out.println(res.getStatus() + " " + res.getBody());
	}

La función **createPlaylist_Sync()** será la encargada de crear una playlist vacía, que posteriormente podremos rellenar. Está basada en la librería Unirest, cuya función es poder manejar peticiones HTTP.

Para implementar esta funcionalidad debemos rellenar el cuerpo (body) de la petición con información sobre la nueva playlist como el nombre, descripción y el carácter de esta, pudiendo ser pública o privada.

El tipo de petición HTTP será un POST al _endopoint_ https://api.spotify.com/v1/users/{user_id}/playlists; además se añadirán las cabeceras "Accept: application/json", "host, api.spotify.com", "Content-Type: application/json"y "Authorization: Bearer at" con el método header().

Para poder acceder a las peticiones HTTP de la API Web de Spotify debemos solicitar un token de acceso a través de la página de Spotify for developers: https://developer.spotify.com/console/post-playlists/. Lo que hemos denominado "String at" en nuestro código.
Para que la petición se ejecute se invoca al método "asString()".

**Obtener las “10 top tracks” de unos artistas**

	
	public static String[] getArtistsTopTracks_Sync() {
		clientCredentials_Sync();
		String artistId[] = { "5Q30fhTc0Sl0Q4C5dOjhhQ", "21ttsUKZ3y2Hm6nduyvbAw" };
		spotifyApi = new SpotifyApi.Builder().setAccessToken(accessToken).build();

		String[] ids_canciones1 = null;
		String[] ids_canciones2 = null;
		for (int i = 0; i < artistId.length; i++) {
			getArtistsTopTracksRequest = spotifyApi.getArtistsTopTracks(artistId[i], countryCode).build();

			try {
				final Track[] tracks = getArtistsTopTracksRequest.execute();
				final int longitud = tracks.length;
				if (i == 0) { 
					ids_canciones1 = new String[longitud];
					for (int j = 0; j < longitud; j++) {
						ids_canciones1[j] = ((Track) tracks[j]).getId();
					}
				}
				if (i == 1) {
					ids_canciones2 = new String[longitud];
					for (int jj = 0; jj < longitud; jj++) {
						ids_canciones2[jj] = ((Track) tracks[jj]).getId(); 
					}
				}
			} catch (IOException | SpotifyWebApiException | ParseException e) {
				System.out.println("Error: " + e.getMessage());
			}
		}
		
		String[] total_ids_canciones = ArrayUtils.addAll(ids_canciones1, ids_canciones2);
		return total_ids_canciones;

	}


Para poder rellenar la playlist creada anteriormente extraeremos las 10 mejores canciones de unos artistas a elección. Para ello hemos implementado la función **getArtistsTopTracks_Sync().**

Los artistas seleccionados vendrán dados por su id, el cual se puede obtener desde la URL del artista: https://open.spotify.com/artist/{artist_id}.

En primer lugar, invocaremos al método clientCredentials_Sync() para poder autenticarnos y así ser capaces de acceder a datos de Spotify. Posteriormente, ya podremos realizar las peticiones a la API (getArtistsTopTracks()) para extraer las canciones solicitadas que iremos guardando en sus correspondientes arrays (ids_cancionesX), los concatenaremos y así, finalmente, la función devolverá una lista con todas las canciones extraídas de los artistas dados por sus ids (total_ids_canciones).

**Rellenar nuestra playlist**

	public static void addTracks(String[] song) {

		String body = "{}";
		String at = 		"BQAWazpPq4E2BX_WTgk2fykB5xFqgpbLPgrtko7-mkHAz37u_EkTjjgQB9T9DJ_Rt9zwXKsblMtPnQgTyqb7RJ5smUsQajl5HJhQQsEsukDffc68UGJ0zw6T-eok1yJgK-YsAYOdiVf2fNRs7HRbtSTJBTXkMmS-eQ4INp_-jlzuAl42-piL8F3pjm6omX9q_zrCUw";
		for (int i = 0; i < song.length; i++) {
			RequestBodyEntity postReq = Unirest
					.post("https://api.spotify.com/v1/playlists/1n9Y4oRiHiM9GA234aCuWE/tracks?uris=spotify%3Atrack%3A"
							+ song[i])
					.header("Accept", "application/json").header("host", "api.spotify.com")
					.header("Authorization", "Bearer " + at).header("Content-Type", "application/json").body(body);

			HttpResponse<String> res = postReq.asString();
			System.out.println(res.getStatus() + " " + res.getBody());
		}

	}


Finalmente, tenemos todos los datos necesarios para añadir las canciones de la función getArtistsTopTracks_Sync() a nuestra playlist creada por la función createPlaylist_Sync(). Para ello se ha implementado la función **addTracks(String[] song)**, cuyo parámetro de entrada será la lista de los ids de las canciones obtenidas anteriormente.
Está función es similar a createPlaylist_Sync(); ya que también utilizaremos la librería Unirest, y así poder manejar peticiones HTTP.

De igual manera será necesario solicitar un token de acceso a Spotify for Developers: https://developer.spotify.com/console/post-playlist-tracks/.

El tipo de petición HTTP será un POST al _endopoint_ https://api.spotify.com/v1/playlists/{playlist_id}/tracks. Pudiendo obtener el id de la playlist desde su URL https://open.spotify.com/playlist/{playlist_id}.



**Bibliografía**

[1] https://github.com/spotify-web-api-java/spotify-web-api-java

[2] API Web de Spotify. https://developer.spotify.com/documentation/web-api/

[3] https://developer.spotify.com/dashboard/
