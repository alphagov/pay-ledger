package uk.gov.pay.ledger.util.csv;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

@Provider
@Produces("text/csv")
public class CSVMessageBodyWriter implements MessageBodyWriter<TransactionSearchResponse> {

    @Override
    public boolean isWriteable(Class targetType, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return targetType == TransactionSearchResponse.class;
    }

    @Override
    public long getSize(TransactionSearchResponse data, Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        // https://docs.oracle.com/javaee/7/api/javax/ws/rs/ext/MessageBodyWriter.html
        return -1;
    }

    @Override
    public void writeTo(TransactionSearchResponse data, Class aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
        if (data != null) {
            List<FlatCsvTransaction> results = data
                    .getTransactionViewList()
                    .stream()
                    .map(FlatCsvTransaction::from)
                    .collect(Collectors.toUnmodifiableList());

            if (!results.isEmpty()) {

                CsvMapper mapper = new CsvMapper();

                // rank properties by class property order instead of alphabetically
                mapper.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
                Object sample = results.get(0);
                CsvSchema schema = mapper.schemaFor(sample.getClass()).withHeader();
                mapper.writer(schema).writeValue(outputStream, results);
            }
        }
    }
}