package org.lunatech.filestreamprocessing.controller;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import org.lunatech.filestreamprocessing.model.ResponseChunk;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class MainController {

    // curl -v -F data=@dataset.csv http://localhost:8080/process

    private static final String SEPARATOR_CHAR = "\n";
    private static final byte[] SEPARATOR_BYTES = SEPARATOR_CHAR.getBytes(UTF_8);
    private static final byte SEPARATOR = SEPARATOR_BYTES[0];
    @PostMapping(path = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_NDJSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<ResponseChunk> process(@RequestBody Flux<PartEvent> allPartsEvents) {
        return allPartsEvents.windowUntil(PartEvent::isLast)
            .concatMap(p -> p.switchOnFirst((signal, partEvents) -> {
                if (signal.hasValue()) {
                    PartEvent event = signal.get();
                    if (event instanceof FilePartEvent fileEvent) {
                        String filename = fileEvent.filename();
                        System.out.println("Processing file " + filename);
                        AtomicInteger counter = new AtomicInteger(0);
                        AtomicInteger skipped = new AtomicInteger(0);
                        CSVParser parser = new CSVParserBuilder()
                                .withSeparator(',')
                                //.withQuoteChar('"')
                                .build();

                        SimpleRegression sr = new SimpleRegression();
                        Flux<ResponseChunk> contents =
                                partEvents.map(PartEvent::content)
                                .flatMapSequential(buf -> this.splitBy(SEPARATOR, buf))
                                .windowUntil(buf -> buf.indexOf(i -> i == SEPARATOR, 0) == 0)
                                // need to join byte buffers before calling to string, UTF8 is multi byte
                                .flatMap(DataBufferUtils::join)
                                .map(buf -> {
                                    var line = buf.toString(UTF_8);
                                    DataBufferUtils.release(buf);
                                    return line;
                                })
                                .skip(1)
                                .map(rawLine -> {
                                    try {
                                        String[] parts = parser.parseLine(rawLine);
                                        String name = parts[0];
                                        String rawDistance = parts[19];
                                        String rawArrDelay = parts[15];
                                        if (rawDistance.isBlank() || rawArrDelay.isBlank()) {
                                            return new ResponseChunk(name, counter.incrementAndGet(), skipped.incrementAndGet());
                                        } else {
                                            double distance = Double.parseDouble(rawDistance);
                                            double arrDelay = Double.parseDouble(rawArrDelay);
                                            sr.addData(distance, arrDelay);
                                            return new ResponseChunk(name, counter.incrementAndGet(), skipped.get());
                                        }
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                        return Flux.concat(contents, Flux.just("done").map(x -> {
                            var predictions = runPredictions(sr);
                            return new ResponseChunk(predictions, -1, -1);
                        }));
                    }
                    else {
                        return Flux.error(new RuntimeException("Unexpected event: " + event));
                    }
                }
                else {
                    return Flux.error(new RuntimeException("Unexpected"));
                }
            }));
    }

    private Flux<DataBuffer> splitBy(Byte separator, DataBuffer buf) {
        if (buf.capacity() <= 0) { // e.g. an empty trailing line should still emit an event
            return Flux.just(buf);
        }
        Flux<DataBuffer> r = Flux.empty();
        int separatorIndex;
        while ((separatorIndex = buf.indexOf(i -> i == separator, 0)) >= 0) {
            r = Flux.concat(r, Flux.just(buf.split(separatorIndex), buf.split(1)));
        }
        return Flux.concat(r, Flux.just(buf)).filter(b -> {
            var filtered = b.capacity() > 0;
            if (!filtered) {
                DataBufferUtils.release(b);
            }
            return filtered;
        });
    }

    private String runPredictions(SimpleRegression sr) {
        StringBuilder sb = new StringBuilder();
        // Display the intercept of the regression
        sb.append("Intercept: " + sr.getIntercept());
        sb.append("\n");
        // Display the slope of the regression.
        sb.append("Slope: " + sr.getSlope());
        sb.append("\n");
        // Display the slope standard error
        sb.append("Standard Error: " + sr.getSlopeStdErr());
        sb.append("\n");
        // Display adjusted R2 value
        sb.append("Adjusted R2 value: " + sr.getRSquare());
        sb.append("\n");
        sb.append("*************************************************");
        sb.append("\n");
        sb.append("Running random predictions......");
        sb.append("\n");
        sb.append("");
        Random r = new Random();
        double[] inputs = new double[]{10, 300, 500, 850, 1500, 3000, 5500, 9000, 15000, 25000};
        for (int i = 0 ; i < inputs.length ; i++) {
            double rn = inputs[i];
            sb.append("Input score: " + rn + " prediction: " + Math.round(sr.predict(rn)));
            sb.append("\n");
        }
        return sb.toString();
    }

}
