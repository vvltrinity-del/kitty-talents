package com.kittytalents.core.component;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.combat.CatRole;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A cat's combat loadout as one value: role, element, three skills and an ultimate.
 * Shared by the client->server "set loadout" packet and the server->client sync so both use the same rules.
 */
public record CatLoadout(CatRole role, FelineElement element, String slot1, String slot2, String slot3, String ultimate) {

    private static final int MAX_ID = 64;

    public static CatLoadout of(TalentedCat cat) {
        return new CatLoadout(cat.getCatRole(), cat.getPrimaryElement(),
                cat.getAbilitySlot1(), cat.getAbilitySlot2(), cat.getAbilitySlot3(), cat.getAbilityUltimate());
    }

    public void applyTo(TalentedCat cat) {
        cat.setCatRole(role);
        cat.setPrimaryElement(element);
        cat.setAbilitySlot1(slot1);
        cat.setAbilitySlot2(slot2);
        cat.setAbilitySlot3(slot3);
        cat.setAbilityUltimate(ultimate);
    }

    /**
     * Server-side validation of a client request: every slot must name a registered ability of the right kind
     * (skills in 1-3, an ultimate in the ultimate slot) and no skill may be equipped twice.
     *
     * @return null if valid, otherwise a short reason
     */
    public String validate() {
        if (role == null || element == null) return "missing role or element";
        for (String id : new String[]{slot1, slot2, slot3}) {
            CatAbility a = CatAbilities.get(id);
            if (a == null) return "unknown ability " + id;
            if (a.getCategory() != CatAbility.Category.SKILL) return id + " is not a skill";
        }
        if (slot1.equals(slot2) || slot1.equals(slot3) || slot2.equals(slot3)) return "duplicate skill";
        CatAbility ult = CatAbilities.get(ultimate);
        if (ult == null || ult.getCategory() != CatAbility.Category.ULTIMATE) return "invalid ultimate " + ultimate;
        return null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(role);
        buf.writeEnum(element);
        buf.writeUtf(slot1, MAX_ID);
        buf.writeUtf(slot2, MAX_ID);
        buf.writeUtf(slot3, MAX_ID);
        buf.writeUtf(ultimate, MAX_ID);
    }

    public static CatLoadout read(FriendlyByteBuf buf) {
        return new CatLoadout(buf.readEnum(CatRole.class), buf.readEnum(FelineElement.class),
                buf.readUtf(MAX_ID), buf.readUtf(MAX_ID), buf.readUtf(MAX_ID), buf.readUtf(MAX_ID));
    }
}
