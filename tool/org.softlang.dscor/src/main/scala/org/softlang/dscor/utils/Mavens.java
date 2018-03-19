package org.softlang.dscor.utils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;


/**
 * Created by Johannes on 12.10.2017.
 */
public class Mavens {
    public static String MAVEN = "http://repo1.maven.org/maven2/";
    // Reading poms: Maven xpp3 reader.

    private final static RepositorySystem system = newRepositorySystem();
    private final static RepositorySystemSession session = newSession(system);
    private final static RemoteRepository repo = new RemoteRepository.Builder("central", "default", MAVEN).build();

    public static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    public static RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository("local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    public static String groupId(String coords) {
        return new DefaultArtifact(coords).getGroupId();
    }

    public static Version version(String coords) {
        return new Version(new DefaultArtifact(coords).getVersion());
    }

//    public static boolean validVersion(String coords) {
//        try {
//            new Version(new DefaultArtifact(coords).getVersion());
//        } catch (IllegalArgumentException e) {
//            return false;
//        }
//        return true;
//    }

    public static String artifactId(String coords) {
        return new DefaultArtifact(coords).getArtifactId();
    }

//    public static boolean exisitsJar(String coords) {
//        try {
//            final URL url = new URL(pathOfJar(groupId(coords), artifactId(coords), version(coords)));
//            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
//            huc.setRequestMethod("HEAD");
//            int responseCode = huc.getResponseCode();
//
//            if (responseCode == 200) {
//                return true;
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return false;
//    }

    public static List<String> findAvailableVersions(String coords) throws Exception {
        return findAvailableVersions(coords, system);
    }

    /**
     * Returns all available versions in the given coords.
     *
     * @param coords (e.g. 'asm:asm:[0,)')
     * @throws Exception
     */
    public static List<String> findAvailableVersions(String coords, RepositorySystem system) throws Exception {

        Artifact artifact = new DefaultArtifact(coords);

        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.addRepository(repo);

        VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);

        return rangeResult.getVersions().stream().map(x -> artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + x.toString()).collect(Collectors.toList());
    }

    public static String pathOfJar(String groupId, String artifactId, Version version) {
        return groupId.replaceAll("\\.", "/") + "/"
                + artifactId.replaceAll("\\.", "/") + "/"
                + version + "/"
                + artifactId + "-" + version + ".jar";
    }


    /**
     * Returns null in case of exception.
     *
     * @param url
     * @return
     */
    public static Optional<Set<String>> category(String url) {
        try {
            Set<String> results = new HashSet<>();
            Document doc = Jsoup.connect(url).timeout(1000 * 30).get();

            for (Element category : doc.select("a.b.c"))
                results.add(category.text());

            return Optional.of(results);
        } catch (HttpStatusException | SocketTimeoutException | ConnectException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns null in case of exception.
     *
     * @param url
     * @return
     */
    public static Optional<Set<String>> tags(String url) {
        try {
            Set<String> results = new HashSet<>();
            Document doc = Jsoup.connect(url).timeout(1000 * 30).get();
            for (Element tag : doc.select("a.b.tag"))
                results.add(tag.text());

            return Optional.of(results);
        } catch (HttpStatusException | SocketTimeoutException | ConnectException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<ZipFile> downloadJar(String coords) {
        return downloadJar(groupId(coords), artifactId(coords), version(coords));
    }

    public static Optional<ZipFile> downloadSource(String coords) {
        return downloadSource(groupId(coords), artifactId(coords), version(coords));
    }

    public static Optional<ZipFile> downloadJar(String groupId, String artifactId, Version version) {
        String path = JUtils.configuration("temp") + "/mavens/" + pathOfJar(groupId, artifactId, version);
        return Optional.ofNullable(download(path, MAVEN + pathOfJar(groupId, artifactId, version)));
    }

    public static Optional<ZipFile> downloadSource(String groupId, String artifactId, Version version) {
        String path = JUtils.configuration("temp") + "/mavens/" + pathOfSource(groupId, artifactId, version);
        return Optional.ofNullable(download(path, MAVEN + pathOfSource(groupId, artifactId, version)));
    }

    public static ZipFile download(String target, String url) {
        File file = new File(target);
        try {
            if (!file.exists())
                FileUtils.copyURLToFile(new URL(url), new File(target));

            ZipFile zipFile = new ZipFile(target);
            return zipFile;
        } catch (IOException e) {
            //System.out.println("Error in: " + url + " exception " + e.toString());
            return null;
        }
    }

    public static String pathOfSource(String groupId, String artifactId, Version version) {
        return groupId.replaceAll("\\.", "/") + "/"
                + artifactId.replaceAll("\\.", "/") + "/"
                + version + "/"
                + artifactId + "-" + version + "-sources.jar";
    }

//    public static void main(String[] args) throws ModelBuildingException, IOException, XmlPullParserException {
//        //Model model = parseGitPom("https://raw.githubusercontent.com/Alogrhythm/Klickkart/1ed65ab6af33ca979bd61274050df5e4b580c1a8/klickakart/pom.xml");
////        Model model = parseGitPom("https://raw.githubusercontent.com/kravinderreddy/camel_splunk/200e097d9df501724b4e7681768562daef3e4359/parent/pom.xml");
////
////        System.out.println(model.getProperties());
////        System.out.println(model.getDependencies());
////        System.out.println(model.getDependencyManagement().getDependencies());
//        System.out.println(category("https://mvnrepository.com/artifact/org.springframework/spring-web/3.2.7.RELEASE"));
//
//    }

//    public static Model parseEffectiveGitPom(String url) throws ModelBuildingException, MalformedURLException {
//        ModelBuildingRequest req = new DefaultModelBuildingRequest();
//        req.setProcessPlugins(false);
//        req.setModelSource(new GitModelSource(new URL(url)));
//        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
//        // TODO: This can not run im parallel!
//        req.setModelResolver(new MavenModelResolver(new MavenRepositorySystem(), session, Collections.singletonList(repo)));
//
//        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
//
//        return builder.build(req).getEffectiveModel();
//    }

    public static Model parseGitPom(String url) throws IOException, XmlPullParserException {
        InputStream inputStream = new URL(url).openStream();
        try {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            return xpp3Reader.read(inputStream);
        } finally {
            inputStream.close();
        }
    }


//    public static void resolveAndCheckout() throws Exception {
//        RepositorySystem repoSystem = newRepositorySystem();
//
//        RepositorySystemSession session = newSession(repoSystem);
//        //Dependency dependency = new Dependency(new DefaultArtifact("gov.nist.math:jama:1.0.3"), "compile");
//        Dependency dependency =
//                new Dependency(new DefaultArtifact("jdom:jdom:1.0"), "compile");
//        RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
//
//        CollectRequest collectRequest = new CollectRequest();
//        collectRequest.setRoot(dependency);
//        collectRequest.addRepository(central);
//        DependencyNode node = repoSystem.collectDependencies(session, collectRequest).getRoot();
//
//        DependencyRequest dependencyRequest = new DependencyRequest();
//        dependencyRequest.setRoot(node);
//
//        repoSystem.resolveDependencies(session, dependencyRequest);
//
//        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
//        node.accept(nlg);
//        System.out.println(nlg.getClassPath());
//    }
}
