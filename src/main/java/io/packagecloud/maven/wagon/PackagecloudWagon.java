package io.packagecloud.maven.wagon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.apache.http.entity.BufferedHttpEntity;
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
                .setUserAgent("io.packagecloud.maven.wagon 0.0.7")
                .build();
    }

    private boolean isDebug = true;

	public PackagecloudWagon() {
        super();
    }

    private void insertKeyValueToBuf(StringBuffer buf, String key, String value){
        buf.append(String.format("\n  %s: %s \n", key, value));
    }

    private void insertSystemPropertyToBuf(StringBuffer buf, String key){
        insertKeyValueToBuf(buf, key, System.getProperty(key));
    }

    private void outputDebug(String str){
        if(isDebug()){
            System.out.println(String.format("[PACKAGECLOUD-DEBUG] %s", str));
        }
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        outputDebug("openConnectionInternal()");
        String packagecloudDebug = System.getProperty("packagecloudDebug");
        if (packagecloudDebug != null && packagecloudDebug.equals("true")) {
           setDebug(true);
            StringBuffer buf = new StringBuffer();
            buf.append("\n\n\n-----DEBUG VARS----\n");
            insertSystemPropertyToBuf(buf, "java.home");
            insertSystemPropertyToBuf(buf, "java.vm.name");
            insertSystemPropertyToBuf(buf, "java.version");
            insertSystemPropertyToBuf(buf, "java.vm.vendor");
            insertSystemPropertyToBuf(buf, "java.vm.version");
            insertSystemPropertyToBuf(buf, "user.home");
            insertKeyValueToBuf(buf, "repository id", getRepository().getId());
            insertKeyValueToBuf(buf, "repository url", getRepository().getUrl());

            if (getRepository().getPermissions() != null){
                String dirMode = getRepository().getPermissions().getDirectoryMode();
                String fileMode = getRepository().getPermissions().getFileMode();
                String group = getRepository().getPermissions().getGroup();
                insertKeyValueToBuf(buf, "dirMode", dirMode);
                insertKeyValueToBuf(buf, "fileMode", fileMode);
                insertKeyValueToBuf(buf, "group", group);
            }

            if (getProxyInfo() != null){
                insertKeyValueToBuf(buf, "proxy.host", getProxyInfo().getHost());
            }

            buf.append("\n-----END DEBUG VARS----\n");
            System.out.println(buf.toString());
        }

    }


    @Override
    public void disconnect() throws ConnectionException {
        outputDebug("disconnect()");
    }

    @Override
    protected void closeConnection() throws ConnectionException {
        outputDebug("closeConnection()");
    }

    public void get(String s, File file) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        outputDebug(String.format("get(): %s", s));
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

            if (statusLine.getStatusCode() == 422) {
                String responseText = IOUtils.toString(response.getEntity().getContent());
                throw new TransferFailedException(String.format("Download failed: %s", responseText));
            }

            if (statusLine.getStatusCode() == 500) {
                throw new TransferFailedException("There was an unexpected server error! (500)");
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
                if(response != null){
                    response.close();
                }
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
        outputDebug(String.format("put: %s", s));
        Resource resource = new Resource(s);
        resource.setContentLength(file.length());
        resource.setLastModified(file.lastModified());

        firePutInitiated(resource, file);
        CloseableHttpResponse response = null;
        try {
            firePutStarted(resource, file);

            HttpPut httpPut = new HttpPut(constructArtifactRequest(s));
            BufferedFileEntity fileEntity = new BufferedFileEntity(file);
            httpPut.setEntity(fileEntity);

            response = httpClient.execute(getTargetHost(), httpPut, getContext());
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == 404) {
                throw new ResourceDoesNotExistException(String.format("Not found %s", s));
            }

            if (statusLine.getStatusCode() == 401) {
                throw new AuthorizationException(String.format("Could not authenticate with %s", getAuthenticationInfo().getPassword()));
            }

            if (statusLine.getStatusCode() == 422) {
                String responseText = IOUtils.toString(response.getEntity().getContent());
                throw new TransferFailedException(String.format("Upload failed: %s", responseText));
            }

            if (statusLine.getStatusCode() == 500) {
                throw new TransferFailedException("There was an unexpected server error! (500)");
            }

            postProcessListeners(resource, file, TransferEvent.REQUEST_PUT);

        } catch (IOException e) {
            throw new TransferFailedException(String.format("Could not transfer %s to %s", s, getTargetHost().getHostName()));
        } catch (URISyntaxException e) {
            throw new TransferFailedException(String.format("Could not construct url %s to %s", s, getTargetHost().getHostName()));
        } finally {
            try {
                if(response != null){
                    response.close();
                }
                firePutCompleted(resource, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private HttpClientContext getContext() throws AuthorizationException {
        HttpClientContext context = HttpClientContext.create();
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(getTargetHost(), basicAuth);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        String password = getAuthenticationInfo().getPassword();
        outputDebug("getContext(): looking up password");
        if (password == null){
            // can't find what what we need in settings.xml
            raiseAndtroubleShootPassword();
        } else {
            credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "basic"),
                new UsernamePasswordCredentials(password, ""));
            context.setCredentialsProvider(credsProvider);
            context.setAuthCache(authCache);
        }
        return context;
    }

    private void raiseAndtroubleShootPassword() throws AuthorizationException {
        StringBuffer buf = new StringBuffer();
        buf.append("\n\n\n-----AUTHENTICATION ERROR-----\n");
        buf.append("Cannot find password for repository id:");
        buf.append(getRepository().getId());
        buf.append(" in settings.xml!\n");
        buf.append("-----AUTHENTICATION ERROR-----\n\n\n");

        throw new AuthorizationException(buf.toString());
    }

    private HttpHost getTargetHost() {
        String protocol = getRepository().getProtocol();
        if (protocol.contains("https")) {
            return new HttpHost(getRepository().getHost(), getRepository().getPort(), "https");
        } else {
            return new HttpHost(getRepository().getHost(), getRepository().getPort(), "http");
        }
    }

    private PackagecloudRepository getPackagecloudRepo() {
        return new PackagecloudRepository(getRepository());
    }

    private String constructArtifactRequest(String key) throws URISyntaxException {
        outputDebug(String.format("constructArtifactRequest(): %s", key));
        PackagecloudRepository packagecloudRepository = getPackagecloudRepo();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key", new File("/", key).toString()));
        URIBuilder builder = new URIBuilder()
                .setParameters(params)
                .setPath(String.format("/api/v1/repos/%s/%s/artifacts.json",
                        packagecloudRepository.getUserName(),
                        packagecloudRepository.getRepoName()
                ));
        return builder.build().toString();
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }
}
