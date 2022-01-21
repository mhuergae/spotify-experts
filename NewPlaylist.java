package cdistfinal;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hc.core5.http.ParseException;
import com.neovisionaries.i18n.CountryCode;

import kong.unirest.HttpResponse;
import kong.unirest.RequestBodyEntity;
import kong.unirest.Unirest;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.artists.GetArtistsTopTracksRequest;

public class NewPlaylist {

	private static String clientId = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
	private static String clientSecret = "XXXXXXXXXXXXXXXXXXXXXXXXXXX";
	private static URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:8080/callback");
	private static SpotifyApi spotifyApi;
	private static ClientCredentialsRequest clientCredentialsRequest;
	private static String accessToken = "";
	private static ClientCredentials clientCredentials = null;
	private static final CountryCode countryCode = CountryCode.SE;
	private static GetArtistsTopTracksRequest getArtistsTopTracksRequest;

	public static void clientCredentials_Sync() {
		try {
			spotifyApi = new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret)
					.setRedirectUri(redirectUri).build();
			clientCredentialsRequest = spotifyApi.clientCredentials().build();
			clientCredentials = clientCredentialsRequest.execute();

			// Set access token for further "spotifyApi" object usage
			accessToken = clientCredentials.getAccessToken();
			spotifyApi.setAccessToken(clientCredentials.getAccessToken());

		} catch (IOException | SpotifyWebApiException | ParseException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	public static void createPlaylist_Sync() {
		String body = "{\r\n" + "  \"name\": \"CDIST FINAL BeaMara\",\r\n"
				+ "  \"description\": \"Playlist para el proyecto final de Computacin Distribuida de Mara Huerga y Beatriz Gil. uc3m.\",\r\n"
				+ "  \"public\": false\r\n" + "}";
		String at = "BQB9J-4GLAUj3716evP8ayApdc1X4gaoZueG5dHFHRFRe2JCaKUk0aG4TGP66CDFVoPdqaTUFuSxQbohWoEuWj2p2AvwbYWn-NzU0OOQhTEHeME4Zx7YioLFK7NmeRN1CeM0kXGLwTmlvf1HevbDqDHx4Z6YENzFnFLcyFNCDSOPQ-CcMinLrjr-2KQ1ow0JIFx27A"; 
		RequestBodyEntity postReq = Unirest.post("https://api.spotify.com/v1/users/USERNAME/playlists")
				.header("Accept", "application/json").header("host", "api.spotify.com")
				.header("Authorization", "Bearer " + at).header("Content-Type", "application/json").body(body);

		HttpResponse<String> res = postReq.asString();

		System.out.println(res.getStatus() + " " + res.getBody());
	}

	
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

	
	public static void addTracks(String[] song) {

		String body = "{}";
		String at = "BQAWazpPq4E2BX_WTgk2fykB5xFqgpbLPgrtko7-mkHAz37u_EkTjjgQB9T9DJ_Rt9zwXKsblMtPnQgTyqb7RJ5smUsQajl5HJhQQsEsukDffc68UGJ0zw6T-eok1yJgK-YsAYOdiVf2fNRs7HRbtSTJBTXkMmS-eQ4INp_-jlzuAl42-piL8F3pjm6omX9q_zrCUw";
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

	public static void main(String[] args) {
		clientCredentials_Sync();
		createPlaylist_Sync(); 
		String[] ids_canciones = getArtistsTopTracks_Sync();
		addTracks(ids_canciones);
	}
}
