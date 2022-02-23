package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Inject;
import uk.gov.pay.ledger.gatewayaccountmetadata.service.GatewayAccountMetadataService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.CsvTransactionFactory;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvService {

    private final CsvTransactionFactory csvTransactionFactory;
    private final GatewayAccountMetadataService gatewayAccountMetadataService;


    @Inject
    public CsvService(CsvTransactionFactory csvTransactionFactory,
                      GatewayAccountMetadataService gatewayAccountMetadataService) {
        this.csvTransactionFactory = csvTransactionFactory;
        this.gatewayAccountMetadataService = gatewayAccountMetadataService;
    }

    public ObjectWriter writerFrom(Map<String, Object> headers) {
        CsvMapper mapper = new CsvMapper();
        mapper.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

        CsvSchema.Builder builder = CsvSchema.builder();
        headers.keySet().forEach(builder::addColumn);

        CsvSchema schema = builder.build().withoutHeader();
        return mapper.writer(schema);
    }

    public Map<String, Object> csvHeaderFrom(TransactionSearchParams searchParams,
                                             boolean includeFeeHeaders,
                                             boolean includeMotoHeader,
                                             boolean includeFeeBreakdownHeaders) {
        List<String> metadataKeys = gatewayAccountMetadataService.getKeysForGatewayAccounts(searchParams.getAccountIds());
        return csvTransactionFactory.getCsvHeadersWithMedataKeys(metadataKeys, includeFeeHeaders, includeMotoHeader, includeFeeBreakdownHeaders);
    }

    public String csvStringFrom(Map<String, Object> headers, ObjectWriter writer) throws JsonProcessingException {
        return csvString(List.of(headers), writer);
    }

    public String csvStringFrom(List<TransactionEntity> page, ObjectWriter writer) throws JsonProcessingException {
        List<Map<String, Object>> csvTransactions = page
                .stream()
                .map(csvTransactionFactory::toMap)
                .collect(Collectors.toList());

        return csvString(csvTransactions, writer);
    }

    private String csvString(List<Map<String, Object>> page, ObjectWriter writer) throws JsonProcessingException {
        return writer.writeValueAsString(page);
    }
}
