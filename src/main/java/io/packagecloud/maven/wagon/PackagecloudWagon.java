package io.packagecloud.maven.wagon;

import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.wagon.*;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.http.entity.mime.content.ByteArrayBody;

public class PackagecloudWagon extends AbstractWagon {
    private static final java.lang.String TOKEN = "2f747effc2698af583a2784c8c4ba92779d4e2381e63f77c";
    private final CloseableHttpClient httpClient = getConfiguredHttpClient();
    private final HttpClientContext context = HttpClientContext.create();
    private final SecureRandom random = new SecureRandom();
    private final HttpHost targetHost = new HttpHost("localhost", 3000, "http");

    private CloseableHttpClient getConfiguredHttpClient() {
        return HttpClients
                .custom()
                .setUserAgent("io.packagecloud.maven.wagon 1.0.0")
                .build();
    }

	public PackagecloudWagon() {
        super();
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "basic"),
                //TODO implement auth
                new UsernamePasswordCredentials(TOKEN, ""));
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
    }

    @Override
    protected void closeConnection() throws ConnectionException {
    }


    public void get(String s, File file) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        StatusLine statusLine;
        //TODO use a url builder
        HttpGet httpGet = new HttpGet(String.format("/api/v1/repos/saldo/hi/artifacts.json?key=%s", s));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(targetHost, httpGet, context);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == 404) {
                throw new ResourceDoesNotExistException(String.format("Not found %s", s));
            }

            HttpEntity entity = response.getEntity();
            System.out.println(String.format("Writing to %s", file.getAbsolutePath()));
            FileUtils.copyInputStreamToFile(entity.getContent(), file);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public boolean getIfNewer(String s, File file, long l) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        return false;
    }

    public void put(File file, String s) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        String strResponse;
        StatusLine statusLine;

        //TODO use a url builder
        HttpPut httpPut = new HttpPut(String.format("/api/v1/repos/saldo/hi/artifacts.json?key=%s", s));

        FileEntity fileEntity = new FileEntity(file);

        httpPut.setEntity(fileEntity);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(targetHost, httpPut, context);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
