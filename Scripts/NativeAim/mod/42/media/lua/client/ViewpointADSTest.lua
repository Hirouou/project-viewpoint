local enabled = true
local ticks = 0

function ViewpointADSCapture()
    local player = getPlayer()
    if not player or not ViewpointADSTest then return end
    local writer = getFileWriter("ViewpointADS-diagnostic.txt", true, false)
    if writer then writer:write(ViewpointADSTest.diagnose(player)); writer:close() end
end

local function update(player)
    if not player or not player:isLocalPlayer() then return end
    if not ViewpointADSTest then return end
    local weapon = player:getPrimaryHandItem()
    local item = weapon and weapon:getFullType() or ""
    local active = enabled and ViewpointADSTest.isViewpointActive()
        and not player:isDead() and not player:isSeatedInVehicle()
        and ViewpointADSTest.hasViewmodel(item)
    ViewpointADSTest.setViewmodelState(player, item, active, player:isAiming())
    if active and player:isAiming() then
        ticks = ticks + 1
        if ticks % 120 == 0 then ViewpointADSCapture() end
    else ticks = 0 end
end

Events.OnPlayerUpdate.Add(update)
Events.OnKeyPressed.Add(function(key)
    if key == Keyboard.KEY_F7 then
        enabled = not enabled
        print("[ViewpointADS] Native body alignment enabled=" .. tostring(enabled))
        update(getPlayer())
    end
end)
Events.OnGameStart.Add(function()
    local player = getPlayer()
    print("[ViewpointADS] Ready 0.4.9: original body, smooth breathing/walking, actual shot recoil. F7 compares alignment.")
end)
