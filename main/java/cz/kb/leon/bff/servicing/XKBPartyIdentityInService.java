package cz.kb.leon.bff.servicing;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class XKBPartyIdentityInService {
    private List<PartyIdIS> partyIdIS;

    @Builder
    @Getter
    @Setter
    public static class PartyIdIS {
        private PartyId partyId;
        private String usg;
    }

    @Builder
    @Getter
    @Setter
    public static class PartyId {
        private String id;
        private IdScheme idScheme;

    }
    @Builder
    @Getter
    @Setter
    public static class IdScheme {
        private String code;
    }
}
