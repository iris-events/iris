package org.iris_events.asyncapi.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.iris_events.asyncapi.runtime.util.StreamUtil;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.exception.ArtifactNotFoundException;
import io.apicurio.registry.rest.v2.beans.ArtifactSearchResults;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.apicurio.registry.rest.v2.beans.SearchedArtifact;
import io.apicurio.registry.rest.v2.beans.SearchedVersion;
import io.apicurio.registry.rest.v2.beans.SortBy;
import io.apicurio.registry.rest.v2.beans.SortOrder;
import io.apicurio.registry.rest.v2.beans.VersionSearchResults;
import io.apicurio.registry.types.ArtifactType;

public class ApicurioClient {
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final String GROUP_ID = "org.iris_events.events";
    public static final String GROUP_ID_CLIENT = "org.iris_events.client.events";
    public static final String GROUP_ID_SNAPSHOTS = "org.iris_events.snapshot.events";

    private static final String SNAPSHOT = "SNAPSHOT";
    private static final String ALPHA = "-alpha-";
    private static final int LIMIT = 20;

    private final RegistryClient client;

    public ApicurioClient(RegistryClient client) {
        this.client = client;
    }

    public void upload(String artifactId, String version, String schema) {
        if (isSnapshotVersion(version) || isAlphaVersion(version)) {
            final var snapshotVersion = String.format("%s-%s", version, TIMESTAMP_FORMAT.format(new Date()));
            upload(artifactId, GROUP_ID_SNAPSHOTS, snapshotVersion, schema);
        } else {
            upload(artifactId, GROUP_ID, version, schema);
        }
    }

    public void uploadClientSchema(String artifactId, String version, String schema) {
        upload(artifactId, GROUP_ID_CLIENT, version, schema);
    }

    public List<String> getArtifactsInGroup(String groupId) {
        ArtifactSearchResults artifactSearchResults = client.listArtifactsInGroup(groupId);
        List<SearchedArtifact> searchResults = artifactSearchResults.getArtifacts();
        if (searchResults.isEmpty()) {
            return new ArrayList<>();
        }
        return getLatestArtifacts(searchResults);
    }

    public String getLatestClientArtifact(String artifactId) throws IOException {
        return getLatestArtifact(GROUP_ID_CLIENT, artifactId);
    }

    public String getLatestArtifact(String groupId, String artifactId) throws IOException {
        try {
            return StreamUtil.toString(client.getLatestArtifact(groupId, artifactId));
        } catch (ArtifactNotFoundException e) {
            return null;
        }
    }

    public List<SearchedArtifact> searchArtifacts(String groupId, String name, String description) {
        List<String> labels = null;
        List<String> properties = null;
        SortBy orderBy = SortBy.name;
        SortOrder order = SortOrder.asc;
        int offset = 0;

        List<SearchedArtifact> artifacts = new ArrayList<>();
        boolean moreLeft = true;

        while (moreLeft) {
            ArtifactSearchResults artifactSearchResults = client.searchArtifacts(groupId, name, description, labels, properties,
                    orderBy, order, offset, LIMIT);
            artifacts.addAll(artifactSearchResults.getArtifacts());
            moreLeft = artifactSearchResults.getArtifacts().size() >= LIMIT;
            offset += LIMIT;
        }

        return artifacts;
    }

    public List<SearchedVersion> listArtifactVersions(String groupId, String artifactId) {
        int offset = 0;
        boolean moreLeft = true;

        List<SearchedVersion> versionList = new ArrayList<>();

        while (moreLeft) {
            VersionSearchResults versionSearchResults = client.listArtifactVersions(groupId, artifactId, offset, LIMIT);
            List<SearchedVersion> versions = versionSearchResults.getVersions();
            versionList.addAll(versions);
            moreLeft = versions.size() >= LIMIT;
            offset += LIMIT;
        }
        return versionList;
    }

    private void upload(String artifactId, String groupId, String version, String schema) {
        ByteArrayInputStream dataStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
        client.createArtifact(groupId, artifactId, version, ArtifactType.ASYNCAPI, IfExists.UPDATE, true, dataStream);
    }

    private boolean isSnapshotVersion(String version) {
        return version != null && version.toUpperCase().endsWith(SNAPSHOT);
    }

    private boolean isAlphaVersion(String version) {
        return version != null && version.contains(ALPHA);
    }

    private List<String> getLatestArtifacts(List<SearchedArtifact> searchResults) {
        return searchResults.stream()
                .map(artifact -> client.getLatestArtifact(artifact.getGroupId(), artifact.getId()))
                .map(StreamUtil::toString)
                .collect(Collectors.toList());
    }

}
