package com.guardmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class GuardCommonConfig {
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.IntValue VALIDATION_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.BooleanValue RESCAN_ON_SERVER_START;
    public static final ForgeConfigSpec.BooleanValue VERBOSE_KICK_REASON;

    public static final ForgeConfigSpec.ConfigValue<String> MODS_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> SERVER_REQUIRED_EXCLUDES;
    public static final ForgeConfigSpec.ConfigValue<String> IGNORED_CLIENT_MOD_IDS;
    public static final ForgeConfigSpec.BooleanValue ENFORCE_EXACT_VERSION;
    public static final ForgeConfigSpec.BooleanValue ENFORCE_EXACT_HASH;
    public static final ForgeConfigSpec.BooleanValue ALLOW_UNKNOWN_CLIENT_MODS;
    public static final ForgeConfigSpec.ConfigValue<String> RESOURCEPACKS_MODE;
    public static final ForgeConfigSpec.BooleanValue CHECK_RESOURCEPACK_RAW_HASH_TOO;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_AT_LEAST_ONE_ALLOWED_RESOURCEPACK;
    public static final ForgeConfigSpec.BooleanValue WARN_ON_UNKNOWN_INSTALLED_RESOURCEPACKS;
    public static final ForgeConfigSpec.ConfigValue<String> SHADERPACKS_MODE;
    public static final ForgeConfigSpec.BooleanValue CHECK_SHADERPACK_RAW_HASH_TOO;
    public static final ForgeConfigSpec.BooleanValue WARN_ON_UNKNOWN_INSTALLED_SHADERPACKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        DEBUG = builder
                .comment("Enable additional debug logging.")
                .define("debug", false);

        VALIDATION_TIMEOUT_SECONDS = builder
                .comment("How many seconds the client has to respond with its environment report.")
                .defineInRange("validationTimeoutSeconds", 5, 1, 60);

        RESCAN_ON_SERVER_START = builder
                .comment("Reserved for the next stage. Will control automatic rescan on server startup.")
                .define("rescanOnServerStart", true);

        VERBOSE_KICK_REASON = builder
                .comment("Show more detailed kick reasons to the player.")
                .define("verboseKickReason", true);
        builder.pop();

        builder.push("mods");
        MODS_MODE = builder
                .comment("Current modes: OFF, LENIENT, STRICT, ALLOWLIST_ONLY")
                .define("mode", "STRICT");

        SERVER_REQUIRED_EXCLUDES = builder
                .comment("Comma-separated mod ids that should be ignored when scanning server-required mods.")
                .define("serverRequiredExcludes", "");

        IGNORED_CLIENT_MOD_IDS = builder
                .comment("Comma-separated client mod ids that should be ignored during unknown-client-mod checks. Useful for bundled helper dependencies like mixinextras.")
                .define("ignoredClientModIds", "mixinextras");

        ENFORCE_EXACT_VERSION = builder
                .comment("Require exact client mod version match.")
                .define("enforceExactVersion", true);

        ENFORCE_EXACT_HASH = builder
                .comment("Require exact client jar hash match when the client report contains a hash.")
                .define("enforceExactHash", true);

        ALLOW_UNKNOWN_CLIENT_MODS = builder
                .comment("Allow unknown client mods. In STRICT mode this should normally stay false.")
                .define("allowUnknownClientMods", false);
        builder.pop();

        builder.push("resourcepacks");
        RESOURCEPACKS_MODE = builder
                .comment("Current modes: OFF, ACTIVE_ALLOWLIST, STRICT_ACTIVE_ALLOWLIST")
                .define("mode", "STRICT_ACTIVE_ALLOWLIST");

        CHECK_RESOURCEPACK_RAW_HASH_TOO = builder
                .comment("Compare raw zip hash too when the client sends it.")
                .define("checkRawHashToo", false);

        REQUIRE_AT_LEAST_ONE_ALLOWED_RESOURCEPACK = builder
                .comment("Require the client to have at least one allowed resource pack active.")
                .define("requireAtLeastOneAllowedPack", false);

        WARN_ON_UNKNOWN_INSTALLED_RESOURCEPACKS = builder
                .comment("Warn when the client has installed external resource packs that are not in the allowlist, but do not kick for that alone.")
                .define("warnOnUnknownInstalledResourcePacks", true);
        builder.pop();

        builder.push("shaderpacks");
        SHADERPACKS_MODE = builder
                .comment("Current modes: OFF, ACTIVE_ALLOWLIST, STRICT_ACTIVE_ALLOWLIST")
                .define("mode", "STRICT_ACTIVE_ALLOWLIST");

        CHECK_SHADERPACK_RAW_HASH_TOO = builder
                .comment("Compare raw zip hash too when the client sends it for shader packs.")
                .define("checkRawHashToo", false);

        WARN_ON_UNKNOWN_INSTALLED_SHADERPACKS = builder
                .comment("Warn when the client has installed external shader packs that are not in the allowlist, but do not kick for that alone.")
                .define("warnOnUnknownInstalledShaderPacks", true);
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private GuardCommonConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

}
