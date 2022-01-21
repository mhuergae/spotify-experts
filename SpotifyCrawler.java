package cdistfinal;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistRequest;
import se.michaelthelin.spotify.SpotifyHttpManager;
import org.apache.hc.core5.http.ParseException;
import java.net.URI;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

public class SpotifyCrawler implements Serializable {

	private static final long serialVersionUID = 7975463865195220019L;
	private String clientId = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
	private String clientSecret = "XXXXXXXXXXXXXXXXXXXXXXXXXXXX";
	private URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:8080/callback");
	private SpotifyApi spotifyApi;
	private ClientCredentialsRequest clientCredentialsRequest;
	private ClientCredentials clientCredentials = null;

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

	public static void main(String[] args) {

		SpotifyCrawler sc = new SpotifyCrawler();
		sc.clientCredentials_Sync();
		sc.getDailyMixAnalysis_Sync();
		SparkLib sl = new SparkLib();
		sl.sparkDailyMixAnalysis();

	}

}
