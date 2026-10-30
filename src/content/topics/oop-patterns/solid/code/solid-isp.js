// In JS, ISP shows up as "accept the smallest shape you need".
// ✗ Requires a whole repository object just to read one user.
async function greeting(userRepo, id) {
  const u = await userRepo.findById(id);
  return `Hi ${u.name}`;
}

// ✓ Ask only for the capability you use — trivially fakeable in tests.
export async function greetingFor(findUser, id) {
  const u = await findUser(id);
  return `Hi ${u.name}`;
}

// greetingFor((id) => repo.findById(id), 42)
// greetingFor(async () => ({ name: 'Test' }), 1)   // test double, no mocking library
