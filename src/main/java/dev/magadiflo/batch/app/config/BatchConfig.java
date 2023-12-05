package dev.magadiflo.batch.app.config;

import dev.magadiflo.batch.app.repositories.StudentRepository;
import dev.magadiflo.batch.app.student.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StudentRepository studentRepository;

    // 1. ItemReader: Leer
    @Bean
    public FlatFileItemReader<Student> itemReader() {
        FlatFileItemReader<Student> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource("src/main/resources/students.csv"));
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    // 2. ItemProcessor: Procesar
    @Bean
    public StudentProcessor processor() {
        return new StudentProcessor();
    }

    // 3. ItemWriter: Escribir o guardar la salida
    @Bean
    public RepositoryItemWriter<Student> write() {
        RepositoryItemWriter<Student> writer = new RepositoryItemWriter<>();
        writer.setRepository(this.studentRepository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public Step importStep() {
        return new StepBuilder("csvImport", this.jobRepository)
                .<Student, Student>chunk(10, this.platformTransactionManager) // Cuántos registros o líneas queremos procesar a la vez
                .reader(this.itemReader())
                .processor(this.processor())
                .writer(this.write())
                .taskExecutor(this.taskExecutor())
                .build();
    }

    @Bean
    public Job runJob() {
        return new JobBuilder("importStudents", this.jobRepository)
                .start(this.importStep())
                .build();
    }

    /**
     * Para mejorar la ejecución de las tareas. Cuando ejecutamos la importación del .csv con 100_000 registros,
     * el tiempo aproximado duró 5m32s. Ahora, con este @Bean TaskExecutor, esperamos reducir el tiempo de importación,
     * ya que definiremos como máximo 10 concurrencias.
     *
     * Luego de ejecutar la aplicación y volver a importar los 100_000 registros, el tiempo que tardó fue de 31s558ms,
     * de este modo se redujo considerablemente la importación.
     */
    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setConcurrencyLimit(10); //Debemos tener cuidado con este valor, dependerá de la capacidad de la máquina
        return asyncTaskExecutor;
    }

    private LineMapper<Student> lineMapper() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id", "firstname", "lastname", "age");

        BeanWrapperFieldSetMapper<Student> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Student.class); //Lo usaremos para convertir cada lineTokenizer en un Student

        DefaultLineMapper<Student> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        return lineMapper;
    }
}
