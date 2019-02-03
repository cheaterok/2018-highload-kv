id = 0

wrk.method = "PUT"
wrk.body = "abcdefghijklmnopqrstuvwxyz"

function request()
  path = "/v0/entity?id=" .. id
  id = id + 1
  return wrk.format(nil, path)
end
