package com.jagame.ibm.test.supplier.infrastructure;

import com.jagame.ibm.test.supplier.domain.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;

@Configuration
public class SupplierBatchConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupplierBatchConfig.class);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private ApplicationArguments arguments;

    @Bean
    public FlatFileItemWriter<Supplier> writer() {
        FlatFileItemWriter<Supplier> flatFileWriter = new FlatFileItemWriter<>();
        flatFileWriter.setResource(new FileSystemResource("supplier.csv"));
        flatFileWriter.setLineAggregator(lineAggregator());
        flatFileWriter.setHeaderCallback(writer -> writer.write("id_proveedor;nombre;fecha_alta;id_cliente"));
        return flatFileWriter;
    }

    private <T> LineAggregator<T> lineAggregator() {
        BeanWrapperFieldExtractor<T> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"idProveedor", "nombre", "fechaAlta", "idCliente"});

        DelimitedLineAggregator<T> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(";");
        lineAggregator.setFieldExtractor(fieldExtractor);

        return lineAggregator;
    }

    @Bean
    public JdbcCursorItemReader<Supplier> reader() {
        JdbcCursorItemReader<Supplier> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id_proveedor, nombre, fecha_alta, id_cliente FROM Proveedor WHERE id_cliente=" + arguments.getNonOptionArgs().get(0));
        reader.setRowMapper(new BeanPropertyRowMapper<>(Supplier.class));
        return reader;
    }

    @Bean
    public Tasklet checkRecordsTasklet(JdbcTemplate jdbcTemplate) {
        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                String from = "FROM Proveedor WHERE id_cliente=" + arguments.getNonOptionArgs().get(0);
                int recordsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) " + from, Integer.class);
                if (recordsCount == 0) {
                    LOGGER.error("No records found for processing.");
                    return RepeatStatus.FINISHED;
                }

                var itemsList = jdbcTemplate.query("SELECT id_proveedor, nombre, fecha_alta, id_cliente " + from, new BeanPropertyRowMapper<>(Supplier.class));
                var writer = writer();
                writer.open(contribution.getStepExecution().getExecutionContext());
                writer.write(new Chunk<>(itemsList));

                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    public Job migrateUserJob() {
        Step step = new StepBuilder("migrateSuppliersStep", jobRepository)
                .tasklet(checkRecordsTasklet(new JdbcTemplate(dataSource)), new JdbcTransactionManager(dataSource))
                .build();

        return new JobBuilder("migrateSuppliersJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

}
