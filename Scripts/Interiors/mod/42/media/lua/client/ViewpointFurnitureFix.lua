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
        print("[ViewpointFurnitureFix] 0.1.18 local test registered: roof completion, exterior wall visibility, Potato compatibility and new street models; native window frames and transparent glass; mattress-height bedside table, backed trailer shelf, trailer sink cutouts, and double-sided native foliage cards.")
        loggedSuccess = true
    end
    return true
end

-- Java exposure may happen before or after the client scripts are read.
-- Native registration is idempotent; re-register on world entry after cache resets.
registerPack(false)
Events.OnGameBoot.Add(function() registerPack(false) end)
Events.OnGameStart.Add(function() registerPack(true) end)

-- Keep moveable placement aligned with the raised 3D bedside table mesh.
-- Vanilla placement already uses renderYOffset + the sprite's Surface value.
local bedsideSurfaceBase = {}
local bedsideSurfaceAligned = false
local function alignBedsideTableSurface()
    if bedsideSurfaceAligned then return end
    local adjusted = 0
    for index = 52, 55 do
        local name = "furniture_storage_01_" .. tostring(index)
        local sprite = getSprite and getSprite(name) or nil
        local props = sprite and sprite:getProperties() or nil
        local surface = props and props:has("Surface") and tonumber(props:get("Surface")) or nil
        if surface ~= nil then
            if bedsideSurfaceBase[name] == nil then bedsideSurfaceBase[name] = surface end
            props:set("Surface", tostring(bedsideSurfaceBase[name] + 8))
            adjusted = adjusted + 1
        end
    end
    if adjusted == 4 then
        bedsideSurfaceAligned = true
        print("[ViewpointFurnitureFix] Bedside-table Surface raised by 8 native units for sprites 52-55.")
    end
end
Events.OnGameBoot.Add(alignBedsideTableSurface)
Events.OnGameStart.Add(alignBedsideTableSurface)
