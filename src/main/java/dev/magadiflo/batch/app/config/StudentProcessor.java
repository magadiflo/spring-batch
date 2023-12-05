package dev.magadiflo.batch.app.config;

import dev.magadiflo.batch.app.student.Student;
import org.springframework.batch.item.ItemProcessor;

/**
 * Para el ejemplo, la interfaz ItemProcessor<I,O> recibe como input un Student y
 * como Output devuelve un Student, pero en el mundo real, podríamos recibir un
 * objeto y luego como salida devolver uno distinto
 */
public class StudentProcessor implements ItemProcessor<Student, Student> {
    @Override
    public Student process(Student student) throws Exception {
        /**
         * Aquí va toda la lógica de negocio, para transformar sus datos o procesar sus datos.
         */
        student.setId(null); // Como nuestro archivo .csv viene con id, lo seteamos a null, para que el mismo hibernate le cree automáticamente un valor.
        return student;
    }
}
