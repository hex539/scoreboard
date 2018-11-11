package me.hex539.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

public class RestClient<Self extends RestClient> {
  private final String url;
  private final OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(1, TimeUnit.MINUTES)
      .readTimeout(1, TimeUnit.MINUTES)
      .build();

  private static class Auth {
    final String username;
    final String password;

    public Auth(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }
  private Auth auth;

  public RestClient(final String url) {
    this.url = url;
  }

  public Self setCredentials(String username, String password) {
    auth = new Auth(username, password);
    return (Self) this;
  }

  @FunctionalInterface
  public interface ResponseHandler<T, R> {
    R apply(Optional<T> t) throws IOException;
  }

  public <T> T requestFrom(String endpoint, ResponseHandler<? super String, T> handler)
        throws CompletionException {
    Request.Builder request = new Request.Builder()
        .url(url + endpoint);

    if (auth != null) {
      request.header("Authorization", Credentials.basic(auth.username, auth.password));
    }
    System.err.println("Fetching " + url + endpoint + "...");

    try (Response response = client.newCall(request.build()).execute()) {
      switch  (response.code()) {
        case 200:
          // OK
          final String body = response.body().string();
          try {
            return handler.apply(Optional.ofNullable(body));
          } catch (Exception e) {
            System.err.println("Failed on response body:\n" + body);
            throw e;
          }

        case 401: // Unauthorized (returned by CLICS v1.0)
        case 403: // Forbidden (need to authenticate, older api versions)
        case 405: // Method not allowed (need to authenticate, newer api versions)
          return handler.apply(Optional.empty());
        default: // Not handled. Probably an invalid request.
          throw new IOException(
              "GET " + endpoint + ": " + response.code() + ", " + response.message());
      }
    } catch (IOException e) {
      throw new CompletionException(e);
    }
  }
}
