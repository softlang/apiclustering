package org.softlang.dscor.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;
import com.google.common.io.Files;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by Johannes on 12.10.2017. General Utils.
 */
public class JUtils {
    private static Properties CONFIGURATION = new Properties();

    static {
        try {
            CONFIGURATION.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.out.println("WARNING - There is no 'config.properties' file in the projects root folder");
        }
    }

    public static String configuration(String key) {
        String result = CONFIGURATION.getProperty(key);
        if (result != null)
            return result;

        throw new RuntimeException("Missing configuration key: " + key);
    }


    /**
     * Reads csv as a list of maps. Returns an empty list if file not found.
     *
     * @param file .csv
     * @return
     */
    public static List<Map<String, String>> readCsv(File file) {
        try {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(new FileReader(file));
            List<Map<String, String>> result = new ArrayList<>();
            for (CSVRecord record : records)
                result.add(record.toMap());

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public static List<Map<String, String>> readCsv(List<String> rows) {
//        List<String> header = Lists.newArrayList(Splitter.on(",").split(rows.remove(0)));
//
//        List<Map<String, String>> results = new ArrayList<>();
//        for (String row : rows) {
//            List<String> cells = Lists.newArrayList(Splitter.on(",").split(row));
//            Map<String, String> cs = new HashMap<>();
//            for (int i = 0; i < cells.size(); i++)
//                cs.put(header.get(i), cells.get(i));
//            results.add(cs);
//        }
//        return results;
//    }

    /**
     * Creates a sink that writes csv rows line by line. The csv header is not located in the first line but in a
     * separate '.header' file to avoid previously fixed column.
     *
     * @param file    e.g: content.csv
     * @param charset
     * @return
     */
    public static CSVSink asCSVSink(File file, Charset charset, CSVSink.SinkType type, String... header) {
        return new CSVSink(file.getAbsolutePath(), charset, type, header);
    }

    public static void writeList(File file, Charset charset, Iterable<String> items) {
        try {
            Files.asCharSink(file, charset).write(Joiner.on("\n").join(items));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static boolean exists(URL url) throws IOException {

        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestMethod("HEAD");
        int responseCode = huc.getResponseCode();

        if (responseCode == 200) {
            return true;
        }

        return false;
    }


    public static List<String> readList(File file, Charset charset) {
        try {
            return FileUtils.readLines(file, charset);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }


//    public static boolean writeCsv(File file, List<Map<String, String>> data) {
//        try {
//            FileWriter output = new FileWriter(file);
//
//            List<String> header = Lists.newArrayList(data.stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet()));
//
//            output.write(Joiner.on(",").join(header) + "\n");
//            for (Map<String, String> row : data) {
//                List<String> list = header.stream().map(x -> row.getOrDefault(x, "")).collect(Collectors.toList());
//                output.write(Joiner.on(",").join(list) + "\n");
//                output.flush();
//            }
//
//            output.close();
//            return true;
//        } catch (IOException e) {
//            System.out.println("Can not flush csv results caused by IOException");
//            return false;
//        }
//    }

    public static <T, F> Optional<F> runWithTimeout(Function<T, F> function, T input, long timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Optional<F> result = Optional.empty();
        Future<F> future = executor.submit(() -> function.apply(input));

        try {
            result = Optional.of(future.get(timeout, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executor.shutdownNow();
        return result;
    }

//    public static Long countFiles(File file) {
//        return Files.fileTreeTraverser().postOrderTraversal(file).stream().count();
//    }
}
