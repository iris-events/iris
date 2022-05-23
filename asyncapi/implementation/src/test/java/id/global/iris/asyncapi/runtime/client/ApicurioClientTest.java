package id.global.iris.asyncapi.runtime.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import id.global.iris.asyncapi.runtime.util.StreamUtil;
import io.apicurio.registry.rest.client.impl.RegistryClientImpl;
import io.apicurio.registry.rest.v2.beans.ArtifactSearchResults;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.apicurio.registry.rest.v2.beans.SortBy;
import io.apicurio.registry.rest.v2.beans.SortOrder;
import io.apicurio.registry.rest.v2.beans.VersionSearchResults;
import io.apicurio.registry.types.ArtifactType;

class ApicurioClientTest {

    private static final String ASYNCAPI_FILE = "src/test/resources/asyncapi_merge_1.json";

    private RegistryClientImpl apicurioClientMock;
    private ApicurioClient client;

    @BeforeEach
    private void setup() {
        this.apicurioClientMock = Mockito.mock(RegistryClientImpl.class);
        this.client = new ApicurioClient(apicurioClientMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ApicurioClient.GROUP_ID,
            ApicurioClient.GROUP_ID_SNAPSHOTS,
            ApicurioClient.GROUP_ID_CLIENT
    })
    void upload(String groupId) throws IOException {
        String version = "1.0.0";
        String testArtifactName = "test-artifact";
        if (groupId.equals(ApicurioClient.GROUP_ID_SNAPSHOTS)) {
            version = "1.0.0-SNAPSHOT";
        }

        String fileString = Files.readString(Path.of(ASYNCAPI_FILE));

        if (groupId.equals(ApicurioClient.GROUP_ID_CLIENT)) {
            client.uploadClientSchema(testArtifactName, version, fileString);
        } else {
            client.upload(testArtifactName, version, fileString);
        }

        final var groupIdCaptor = ArgumentCaptor.forClass(String.class);
        final var artifactIdCaptor = ArgumentCaptor.forClass(String.class);
        final var versionCaptor = ArgumentCaptor.forClass(String.class);
        final var artifactTypeCaptor = ArgumentCaptor.forClass(ArtifactType.class);
        final var ifExistsCaptor = ArgumentCaptor.forClass(IfExists.class);
        final var canonicalCaptor = ArgumentCaptor.forClass(Boolean.class);
        final var inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);

        verify(apicurioClientMock)
                .createArtifact(groupIdCaptor.capture(), artifactIdCaptor.capture(), versionCaptor.capture(),
                        artifactTypeCaptor.capture(), ifExistsCaptor.capture(), canonicalCaptor.capture(),
                        inputStreamCaptor.capture());

        String capturedGroupId = groupIdCaptor.getValue();
        String capturedArtifactId = artifactIdCaptor.getValue();
        String capturedVersion = versionCaptor.getValue();
        InputStream capturedSchemaStream = inputStreamCaptor.getValue();

        assertThat(capturedGroupId, is(groupId));
        assertThat(capturedArtifactId, is(testArtifactName));
        if (groupId.equals(ApicurioClient.GROUP_ID_SNAPSHOTS)) {
            assertThat(capturedVersion.startsWith(version), is(true));
        } else {
            assertThat(capturedVersion, is(version));
        }
        assertThat(capturedSchemaStream, is(notNullValue()));
        MatcherAssert.assertThat(StreamUtil.toString(capturedSchemaStream), is(fileString));
    }

    @Test
    void getArtifactsInGroup() {
        when(apicurioClientMock.listArtifactsInGroup(any())).thenReturn(new ArtifactSearchResults());
        client.getArtifactsInGroup("id.global.test.test");
        final var groupIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(apicurioClientMock).listArtifactsInGroup(groupIdCaptor.capture());
        assertThat(groupIdCaptor.getValue(), is("id.global.test.test"));
    }

    @Test
    void getLatestClientArtifact() throws IOException {
        String emptyArtifact = "{}";
        when(apicurioClientMock.getLatestArtifact(any(), any())).thenReturn(new ByteArrayInputStream(emptyArtifact.getBytes()));
        String artifactId = "test-client-artifact-id";
        final var latestClientArtifact = client.getLatestClientArtifact(artifactId);

        final var artifactIdCaptor = ArgumentCaptor.forClass(String.class);
        final var groupIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(apicurioClientMock).getLatestArtifact(groupIdCaptor.capture(), artifactIdCaptor.capture());
        assertThat(groupIdCaptor.getValue(), CoreMatchers.is(ApicurioClient.GROUP_ID_CLIENT));
        assertThat(artifactIdCaptor.getValue(), is(artifactId));
        assertThat(latestClientArtifact, is(emptyArtifact));
    }

    @Test
    void getLatestArtifact() throws IOException {
        String emptyArtifact = "{}";
        when(apicurioClientMock.getLatestArtifact(any(), any())).thenReturn(new ByteArrayInputStream(emptyArtifact.getBytes()));
        String groupId = "id.global.test";
        String artifactId = "test-artifact-id";
        final var latestClientArtifact = client.getLatestArtifact(groupId, artifactId);

        final var artifactIdCaptor = ArgumentCaptor.forClass(String.class);
        final var groupIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(apicurioClientMock).getLatestArtifact(groupIdCaptor.capture(), artifactIdCaptor.capture());
        assertThat(groupIdCaptor.getValue(), is(groupId));
        assertThat(artifactIdCaptor.getValue(), is(artifactId));
        assertThat(latestClientArtifact, is(emptyArtifact));
    }

    @Test
    void searchArtifacts() {
        when(apicurioClientMock.searchArtifacts(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(
                new ArtifactSearchResults());

        String groupId = "id.global.test.test";
        String artifactName = "artifactName";
        String description = "Description of artifact";

        client.searchArtifacts(groupId, artifactName, description);

        final var groupIdCaptor = ArgumentCaptor.forClass(String.class);
        final var nameCaptor = ArgumentCaptor.forClass(String.class);
        final var descriptionCaptor = ArgumentCaptor.forClass(String.class);
        final var labelsCaptor = ArgumentCaptor.forClass((Class<ArrayList<String>>) (Class) ArrayList.class);
        final var propertiesCaptor = ArgumentCaptor.forClass((Class<ArrayList<String>>) (Class) ArrayList.class);
        final var sortByCaptor = ArgumentCaptor.forClass(SortBy.class);
        final var sortOrderCaptor = ArgumentCaptor.forClass(SortOrder.class);
        final var offsetCaptor = ArgumentCaptor.forClass(Integer.class);
        final var limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(apicurioClientMock).searchArtifacts(groupIdCaptor.capture(), nameCaptor.capture(), descriptionCaptor.capture(),
                labelsCaptor.capture(), propertiesCaptor.capture(), sortByCaptor.capture(), sortOrderCaptor.capture(),
                offsetCaptor.capture(), limitCaptor.capture());

        assertThat(groupIdCaptor.getValue(), is(groupId));
        assertThat(nameCaptor.getValue(), is(artifactName));
        assertThat(descriptionCaptor.getValue(), is(description));
        assertThat(labelsCaptor.getValue(), is(nullValue()));
        assertThat(propertiesCaptor.getValue(), is(nullValue()));
        assertThat(sortByCaptor.getValue(), is(SortBy.name));
        assertThat(sortOrderCaptor.getValue(), is(SortOrder.asc));
        assertThat(offsetCaptor.getValue(), is(0));
        assertThat(limitCaptor.getValue(), is(20));
    }

    @Test
    void listArtifactVersions() {
        when(apicurioClientMock.listArtifactVersions(any(), any(), any(), any())).thenReturn(new VersionSearchResults());

        String groupId = "id.global.test.test";
        String artifactName = "artifactName";

        client.listArtifactVersions(groupId, artifactName);

        final var groupIdCaptor = ArgumentCaptor.forClass(String.class);
        final var nameCaptor = ArgumentCaptor.forClass(String.class);
        final var offsetCaptor = ArgumentCaptor.forClass(Integer.class);
        final var limitCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(apicurioClientMock).listArtifactVersions(groupIdCaptor.capture(), nameCaptor.capture(), offsetCaptor.capture(),
                limitCaptor.capture());

        assertThat(groupIdCaptor.getValue(), is(groupId));
        assertThat(nameCaptor.getValue(), is(artifactName));
        assertThat(offsetCaptor.getValue(), is(0));
        assertThat(limitCaptor.getValue(), is(20));
    }
}
