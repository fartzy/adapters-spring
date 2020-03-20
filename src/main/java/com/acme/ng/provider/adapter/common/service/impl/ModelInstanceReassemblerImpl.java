package com.acme.ng.provider.adapter.common.service.impl;

import com.acme.ng.provider.adapter.common.service.ModelInstanceReassembler;
import com.acme.ng.provider.common.repository.document.definitions.ModelDefinitionRepository;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.ModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

/**
 * Created by Julian M on 10/8/18.
 */
@Service
public class ModelInstanceReassemblerImpl implements ModelInstanceReassembler {
    private static final Logger LOG = LoggerFactory.getLogger(ModelInstanceReassemblerImpl.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";

    @Autowired
    private ModelDefinitionRepository modelDefinitionRepository;

    @Autowired
    private MessageSource messageSource;

    @Override
    public ModelInstance reassembleModelInstance(ModelInstance modelInstance) {
        notNull(modelInstance, "ModelInstanceSnapshot shouldn't be NULL");

        LOG.debug("Assembling Model Instance.");

        ModelInstance reassembledModelInstance = new ModelInstance(
                modelDefinitionRepository.findByTableName(modelInstance.getType()));
        reassembledModelInstance.setBusinessData(modelInstance.getBusinessData());
        reassembledModelInstance.setType(modelInstance.getType());

        LOG.debug("Reassembled Model Instance with Id: {}", reassembledModelInstance.getId());

        convertDateTimeValues(reassembledModelInstance);

        return reassembledModelInstance;
    }

    private void convertDateTimeValues(ModelInstance modelInstance) {
        Map<String, TableColumnType> columnsByName = modelInstance.getModelDefinition().getTableColumns().stream()
                .collect(toMap(TableColumnType::getName, column -> column, (original, duplicate) -> original));

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : modelInstance.getBusinessData().entrySet()) {

            if (entry.getValue() == null) {
                continue;
            }

            TableColumnType column = columnsByName.get(entry.getKey());
            notNull(column, "Can't find a column in model definition: " + column);

            Object value = entry.getValue();
            switch (column.getType()) {
                case TIMESTAMP:
                case Timestamp:
                    if (!(value instanceof Instant)) {
                        //Convert to Instant
                        try {
                            value = ConvertStringToInstant ((String) value);
                        } catch (ParseException e) {
                            LOG.error("Error Converting Date to Instant : [{}]",value);
                        }
                    }
                    assertInstant("adapter.convert.timestamp.to.sql.timestamp", value, column, modelInstance);
                    LocalDateTime localDateTime = LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC);
                    value = java.sql.Timestamp.valueOf( localDateTime);
                    break;
                case Date:
                    if (!(value instanceof Instant)) {
                        //Convert to Instant
                        try {
                            value = ConvertStringToInstant ((String) value);
                        } catch (ParseException e) {
                            LOG.error("Error Converting Date to Instant : [{}]",value);
                        }
                    }
                    assertInstant("adapter.convert.timestamp.to.sql.date", value, column, modelInstance);
                    localDateTime = LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC);
                    value = java.sql.Date.valueOf( localDateTime.toLocalDate() );
                    break;
                default: break;
            }

            result.put(entry.getKey(), value);
        }

        modelInstance.setBusinessData(result);
    }

    private void assertInstant(String messageId, Object value, TableColumnType column, ModelInstance modelInstance) {
        String errorMessage = messageSource.getMessage(messageId,
                new Object[] { value, column.getName(), column.getType(), modelInstance.getModelDefinition().getDefID(),
                        Objects.nonNull(value) ? value.getClass().getTypeName() : null },
                Locale.US);
        isTrue(value instanceof Instant, errorMessage);
    }

    private Object ConvertStringToInstant  (String strDate ) throws ParseException {
        DateTimeFormatter isoDateTimeFormat = DateTimeFormatter.ISO_DATE_TIME;
        Instant result = LocalDateTime.parse(strDate, isoDateTimeFormat ).toInstant(ZoneOffset.UTC);
        return result;
    }
}
