let pendingNavigation;

// Several requests can be rejected together. Navigate once without reloading
// the application (which would mount the same requesting components again).
export async function redirectToPasswordChange(router, userStore) {
  userStore.mustChangePwd = true;
  if (router.currentRoute.value.path === "/education-security") return;
  if (!pendingNavigation) {
    pendingNavigation = Promise.resolve()
      .then(() => router.replace("/education-security"))
      .finally(() => {
        pendingNavigation = undefined;
      });
  }
  await pendingNavigation;
}
