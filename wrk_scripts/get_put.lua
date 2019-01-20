id = 0

function request()
	path = "/v0/entity?id=" .. id
  
  if wrk.method == "PUT" then
    id = id + 1

    wrk.method = "GET"
    wrk.body = nil
  else
    wrk.method = "PUT"
    wrk.body = "abcdefghijklmnopqrstuvwxyz"
  end
	
  return wrk.format(nil, path)
end

