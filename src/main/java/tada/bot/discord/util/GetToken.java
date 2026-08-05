package tada.bot.discord.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GetToken {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();
    private static final Logger logger = LoggerFactory.getLogger(GetToken.class);

    static {
        try (InputStream inputStream = GetToken.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                logger.error("Error config not found {} !", CONFIG_FILE);
            } else  {
                properties.load(inputStream);
                logger.info("Loaded config from {}.", CONFIG_FILE);
            }
        } catch (IOException ex) {
            logger.error("Error loading config file!", ex);
        }
    }

    public static String getToken() {
        return properties.getProperty("Token");
    }
}
