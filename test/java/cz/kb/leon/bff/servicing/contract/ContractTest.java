package cz.kb.leon.bff.servicing.contract;

import cz.kb.leon.bff.servicing.TestFeatureFlagConfiguration;
import cz.kb.leon.bff.servicing.TestFeatureFlagService;
import cz.kb.leon.lib.test.utils.contract.BasicSpringBootContractTest;
import cz.kb.speed.integration.jwt.LocalTokenGeneratorKt;
import cz.kb.speed.security.pep.resolver.PepResolver;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Random;

@Slf4j
@BasicSpringBootContractTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestFeatureFlagConfiguration.class)
public abstract class ContractTest {

    private static final String BASE_URI = "http://localhost";
    public static final String AUTHORIZED_PERSON = "KBID=970070122";
    public static final String NOT_PERMITTED_AUTHORIZED_PERSON = "KBID=970070123";
    protected static final String KBID_SCHEMA = "KBID";

    protected static final String X_USERID_IDENTITY_SCHEMA_HEADER = "X-UserId-Identity-Schema";

    static {
        System.setProperty("dynamic.management.server.port", String.valueOf(new Random().nextInt(3000) + 25000));
    }

    @Autowired
    protected TestFeatureFlagService featureFlagService;

    @LocalServerPort
    protected Integer webPort;

    @MockBean
    protected PepResolver pepResolver;

    @BeforeEach
    protected void setUp() {
        RestAssured.baseURI = BASE_URI;
        RestAssured.port = webPort;
    }

    @AfterEach
    void cleanUp() {
        featureFlagService.reset();
    }

    protected String generateTokenFor(String subject) {
        return LocalTokenGeneratorKt.generateToken("https://stage-caas.kb.cz/openam/oauth2", subject, "NDB_MOBILE",
                List.of("ndb_activation", "ndb_mobile_app", "leon.serv:read", "leon.serv:write", "leon.serv:bff", "mujklient_web"), 1);
    }

    protected RequestSpecification given(String authorizedPerson) {
        return RestAssured.given().auth()
                .oauth2(generateTokenFor(authorizedPerson))
                .header("Accept-Language", "cs")
                .header("X-Kb-Session-Id", "sessionId")
                .header("X-Kb-Identity-Schema", "KBID")
                .header("X-Kb-Fe-Platform", "WEB")
                .header("X-Kb-Bus-Channel", "CH0001")
                .header("X-Kb-Fe-Channel", "NDB_INTERNET_BANKING_ASSISTED")
                .header("Origin", "https://contract-test.com");
    }

}
