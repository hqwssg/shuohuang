package cn.com.v2.security;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GoviewSchemaMigration implements ApplicationRunner
{
    private final DataSource dataSource;

    public GoviewSchemaMigration(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement())
        {
            boolean found = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(t_goview_project)"))
            {
                while (columns.next())
                {
                    if ("dept_id".equalsIgnoreCase(columns.getString("name")))
                    {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) statement.execute("ALTER TABLE t_goview_project ADD COLUMN dept_id INTEGER");
        }
    }
}
