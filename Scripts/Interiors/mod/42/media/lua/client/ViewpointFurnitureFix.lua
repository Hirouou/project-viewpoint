-- Register original companion assets through Viewpoint's model-pack API.
-- Viewpoint and ZombieBuddy remain separate dependencies.
local MOD_ID = "ViewpointFurnitureFix"
local MANIFEST = "media/modelpacks/furniturefix/pack.properties"
local loggedSuccess = false
local lastFailure = nil

-- Register only assets shipped with this mod. External model packs are independent.

local function reportFailure(message)
    if lastFailure ~= message then
        print("[ViewpointFurnitureFix] " .. message)
        lastFailure = message
    end
end

local function registerPack(reportMissing)
    local api = Viewpoint and Viewpoint.ModelPacks
    if not api or not api.register then
        if reportMissing then
            reportFailure("Model-pack API unavailable. Enable Viewpoint and ZombieBuddy, then restart the game.")
        end
        return false
    end

    local ok, result = pcall(function()
        return api.register(MOD_ID, MANIFEST)
    end)
    if not ok then
        reportFailure("Registration error: " .. tostring(result))
        return false
    end
    if not result then
        reportFailure("Pack registration failed. Check the enabled mod and its media/modelpacks/furniturefix/pack.properties file.")
        return false
    end

    lastFailure = nil
    if not loggedSuccess then
        print("[ViewpointFurnitureFix] 0.1.19 models-only registered: 1128 model keys / 2057 sprite bindings. Native windows and roofs unchanged; Native Aim is separate.")
        loggedSuccess = true
    end
    return true
end

-- Java exposure may happen before or after the client scripts are read.
-- Native registration is idempotent; re-register on world entry after cache resets.
registerPack(false)
Events.OnGameBoot.Add(function() registerPack(false) end)
Events.OnGameStart.Add(function() registerPack(true) end)
