from django.urls import path

from . import views

urlpatterns = [
    path("", views.index, name="index"),
    path("login", views.login_view, name="login"),
    path("logout", views.logout_view, name="logout"),
    path("register", views.register, name="register"),
    path("create", views.create, name ="create"),
    path("watchlist", views.watchlist, name="watchlist"),
    path("listings/<int:listing_id>", views.listing, name="listing"),
    path("category", views.category_main, name="category_main"),
    path("category/<str:listing_category>", views.category, name="category"),
    path("closed_listings", views.closed_listings, name="closed")
]
