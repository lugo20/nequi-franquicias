package co.com.franquicias.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/** Registers every *UseCase class as a bean so the domain stays free of Spring annotations. */
@Configuration
@ComponentScan(basePackages = "co.com.franquicias.usecase",
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$"),
        useDefaultFilters = false)
public class UseCasesConfig {
}
