package cas.lib.arclib.store;

import cas.lib.arclib.domain.IndexedValidationProfile;
import cas.lib.arclib.domain.QValidationProfile;
import cas.lib.arclib.domain.ValidationProfile;
import org.springframework.stereotype.Repository;

@Repository
public class ValidationProfileStore extends IndexedDatedStore<ValidationProfile, QValidationProfile, IndexedValidationProfile> {
    public ValidationProfileStore() {
        super(ValidationProfile.class, QValidationProfile.class, IndexedValidationProfile.class);
    }
}
