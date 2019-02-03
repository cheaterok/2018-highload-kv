id = 0

direction_up = true

function request()
  path = "/v0/entity?id=" .. id
  
  if id == 50000 or id == -1 then 
    direction_up = not direction_up
  end

  if direction_up then
    wrk.method = "PUT"
    wrk.body = "abcdefghijklmnopqrstuvwxyz"

    id = id + 1
  else
    wrk.method = "GET"
    wrk.body = nil

    id = id - 1
  end
  
  return wrk.format(nil, path)
end
