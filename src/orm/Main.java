package orm;

import orm.annotations.ContextPersistence;
import orm.model.Persona;
import orm.persistence.Persistence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class Main {


    @ContextPersistence
    private Persistence persistence;

    public Persistence getPersistence() {
        return this.persistence;
    }

    public static void main(String[] args) throws Throwable {
        Main m = new Main();
        bind(m);
        var ctx = m.getPersistence();

        try(var in = new BufferedReader(new InputStreamReader(System.in))){
            var flag = true;
            printMenu();
            for(String ln; (ln = in.readLine())!=null;){
                int option = Integer.parseInt(ln);
                if(option!=1)break;

                System.out.println("Ingrese datos de la persona:");
                System.out.println("Nombre: ");
                String name = in.readLine();

                System.out.println("Apellido: ");
                String apellido = in.readLine();

                System.out.println("Edad: ");
                Integer edad = Integer.parseInt(in.readLine());

                var now = LocalDateTime.now();
                System.out.printf("Fecha de registro: %s\n", now);

                var persona = new Persona(name, apellido, edad, now);
                if(flag){
                    ctx.createTable(persona);
                    flag=false;
                }
                ctx.createQuery(persona);
                printMenu();
            }
        }


    }

    public static void bind(Main main) throws InstantiationException, IllegalAccessException {
        var fields = Main.class.getDeclaredFields();
        for (Field field : fields) {
            var member = field.getAnnotation(ContextPersistence.class);
            if (member != null) {
                var type = field.getType();
                var instance = type.newInstance();
                field.setAccessible(true);
                field.set(main, instance);
            }
        }
    }

    private static void printMenu(){
        String menu = """
                        Crear:
                        1. Registro
                        2. Terminar
                        """;
        System.out.println(menu);
    }

}
