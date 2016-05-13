package io.packagecloud.maven.wagon;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class PackagecloudWagon extends AbstractWagon {
    private final CloseableHttpClient httpClient = getConfiguredHttpClient();

    private CloseableHttpClient getConfiguredHttpClient() {
        return HttpClients
                .custom()
                .setUserAgent("io.packagecloud.maven.wagon 1.0.0")
                .build();
    }

	public PackagecloudWagon() {
        super();
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
    }


    @Override
    public void disconnect() throws ConnectionException {
    }

    @Override
    protected void closeConnection() throws ConnectionException {
    }

    public void get(String s, File file) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(s);
        fireGetInitiated(resource, file);
        CloseableHttpResponse response = null;
        try {
            fireGetStarted(resource, file);

            HttpGet httpGet = new HttpGet(constructArtifactRequest(s));
            response = httpClient.execute(getTargetHost(), httpGet, getContext());

            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == 404) {
                throw new ResourceDoesNotExistException(String.format("Not found %s", s));
            }

            if (statusLine.getStatusCode() == 401) {
                throw new AuthorizationException(String.format("Could not authenticate with %s", getAuthenticationInfo().getPassword()));
            }
            HttpEntity entity = response.getEntity();
            FileUtils.copyInputStreamToFile(entity.getContent(), file);
            postProcessListeners(resource, file, TransferEvent.REQUEST_GET);

        } catch (IOException e) {
            throw new TransferFailedException(String.format("Could not transfer %s to %s", s, getTargetHost().getHostName()));
        } catch (URISyntaxException e) {
            throw new TransferFailedException(String.format("Could not construct url %s to %s", s, getTargetHost().getHostName()));
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fireGetCompleted(resource, file);
        }

    }

    public boolean getIfNewer(String s, File file, long l) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        return false;
    }

    public void put(File file, String s) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(s);
        resource.setContentLength(file.length());
        resource.setLastModified(file.lastModified());

        firePutInitiated(resource, file);
        CloseableHttpResponse response = null;
        try {
            firePutStarted(resource, file);

            HttpPut httpPut = new HttpPut(constructArtifactRequest(s));
            FileEntity fileEntity = new FileEntity(file);
            httpPut.setEntity(fileEntity);

            response = httpClient.execute(getTargetHost(), httpPut, getContext());
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == 404) {
                throw new ResourceDoesNotExistException(String.format("Not found %s", s));
            }

            if (statusLine.getStatusCode() == 401) {
                throw new AuthorizationException(String.format("Could not authenticate with %s", getAuthenticationInfo().getPassword()));
            }

        } catch (IOException e) {
            throw new TransferFailedException(String.format("Could not transfer %s to %s", s, getTargetHost().getHostName()));
        } catch (URISyntaxException e) {
            throw new TransferFailedException(String.format("Could not construct url %s to %s", s, getTargetHost().getHostName()));
        } finally {
            try {
                response.close();
                firePutCompleted(resource, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private HttpClientContext getContext() {
        HttpClientContext context = HttpClientContext.create();
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(getTargetHost(), basicAuth);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "basic"),
                new UsernamePasswordCredentials(getAuthenticationInfo().getPassword(), ""));
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);
        return context;
    }

    private HttpHost getTargetHost(){
        return new HttpHost(getRepository().getHost(), getRepository().getPort(), "http");
    }

    private PackagecloudInfo getPackagecloudInfo() {
        String[] strings = getRepository().getBasedir().split("/");
        String packagecloudUser = strings[1];
        String packagecloudRepository = strings[2];
        return new PackagecloudInfo(packagecloudUser, packagecloudRepository);
    }

    private String constructArtifactRequest(String key) throws URISyntaxException {
        PackagecloudInfo packagecloudInfo = getPackagecloudInfo();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key", new File("/", key).toString()));
        URIBuilder builder = new URIBuilder().setParameters(params).setPath(String.format("/api/v1/repos/%s/%s/artifacts.json", packagecloudInfo.getUser(), getPackagecloudInfo().getName()));
        return builder.build().toString();
    }
}
