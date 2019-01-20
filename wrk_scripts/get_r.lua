id = 0
wrk.method = "GET"

direction_up = true

function request()
  path = "/v0/entity?id=" .. id

  if id == 50000 or id == -1 then 
    direction_up = not direction_up
  end

  if direction_up then
    id = id + 1
  else
    id = id - 1
  end

	return wrk.format(nil, path)
end

